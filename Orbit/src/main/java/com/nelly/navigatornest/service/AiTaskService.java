package com.nelly.navigatornest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelly.navigatornest.dto.AiSuggestResponse;
import com.nelly.navigatornest.dto.TaskRequest;
import com.nelly.navigatornest.entity.Task;
import com.nelly.navigatornest.entity.Task.TaskPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiTaskService {

    private final ObjectMapper objectMapper;
    private final TaskService taskService;
    private final DiaryService diaryService;

    @Value("${ai.base-url:http://localhost:11434/v1}")
    private String baseUrl;

    @Value("${ai.model:llama3.2}")
    private String model;

    @Value("${ai.api-key:}")
    private String apiKey;

    private static final String SYSTEM_PROMPT = """
            你是 Navigator Nest 的 AI 任務助理。使用者會描述心情、狀態或想法。
            請根據內容，以繁體中文回覆，且「只輸出合法 JSON」，不要 markdown、不要程式碼區塊、不要多餘說明。

            JSON 格式必須嚴格如下：
            {
              "task": {
                "title": "簡短可執行的任務標題",
                "description": "任務說明（可含小步驟）",
                "priority": "LOW 或 MEDIUM 或 HIGH"
              },
              "diaryText": "一段溫暖、貼近情緒的日記文字（約 80-150 字）",
              "lyricInspiration": "一句或一小段歌詞靈感（可原創、有畫面感）"
            }

            規則：
            - task 要具體、可執行，避免空泛建議
            - priority 只能是 LOW、MEDIUM、HIGH 之一
            - 語氣溫柔、不說教，適合個人成長與創作型使用者
            """;

    // 依使用者輸入呼叫本地 LLM，回傳任務 / 日記 / 歌詞靈感
    // saveToTasks=true：存 Task，並同步存 Diary（關聯 Task）
    // saveToDiary=true：存 Diary（可無 Task）；若同時 saveToTasks 則關聯新建 Task
    public AiSuggestResponse suggest(String userInput, boolean saveToTasks, boolean saveToDiary, Long userId) {
        String content = callChatCompletions(userInput);
        AiSuggestResponse response = parseSuggestion(content);

        Long savedTaskId = null;
        if (saveToTasks && response.getTask() != null) {
            if (userId == null) {
                throw new RuntimeException("userId is required when saveToTasks is true");
            }
            savedTaskId = saveSuggestedTask(userId, response.getTask());
            response.setSavedTaskId(savedTaskId);
        }

        // 存任務時一併存日記；或明確要求只存日記
        boolean shouldSaveDiary = saveToTasks || saveToDiary;
        if (shouldSaveDiary) {
            if (userId == null) {
                throw new RuntimeException("userId is required when saving diary");
            }
            var diary = diaryService.createFromAi(
                    userId,
                    savedTaskId,
                    userInput,
                    response.getDiaryText(),
                    response.getLyricInspiration()
            );
            response.setSavedDiaryId(diary.getId());
        }

        return response;
    }

    /** 相容舊呼叫：不儲存 */
    public AiSuggestResponse suggest(String userInput) {
        return suggest(userInput, false, false, null);
    }

    private Long saveSuggestedTask(Long userId, AiSuggestResponse.SuggestedTask suggested) {
        TaskRequest request = toTaskRequest(suggested);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        Task created = taskService.createTask(userId, task);
        return created.getId();
    }

    private TaskRequest toTaskRequest(AiSuggestResponse.SuggestedTask suggested) {
        TaskRequest request = new TaskRequest();
        request.setTitle(suggested.getTitle());
        request.setDescription(suggested.getDescription());
        request.setPriority(toTaskPriority(suggested.getPriority()));
        request.setDueDate(null);
        return request;
    }

    private TaskPriority toTaskPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return TaskPriority.MEDIUM;
        }
        try {
            return TaskPriority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // AI 可能回傳 URGENT 以外的值；非 LOW/MEDIUM/HIGH/URGENT 時退回 MEDIUM
            return TaskPriority.MEDIUM;
        }
    }

    private String callChatCompletions(String userInput) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        RestClient client = builder.build();

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userInput)
                ),
                "temperature", 0.7,
                "stream", false
        );

        try {
            String raw = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) {
                throw new RuntimeException("AI returned empty response");
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new RuntimeException("AI response missing message content");
            }
            return contentNode.asText();
        } catch (RestClientException e) {
            throw new RuntimeException(
                    "Failed to call local LLM at " + baseUrl + ". Is Ollama / LM Studio running? " + e.getMessage(),
                    e
            );
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM HTTP response: " + e.getMessage(), e);
        }
    }

    private AiSuggestResponse parseSuggestion(String content) {
        try {
            String json = extractJson(content);
            JsonNode node = objectMapper.readTree(json);

            JsonNode taskNode = node.path("task");
            AiSuggestResponse.SuggestedTask task = AiSuggestResponse.SuggestedTask.builder()
                    .title(textOrDefault(taskNode, "title", "今天的一小步"))
                    .description(textOrDefault(taskNode, "description", "先照顧好自己，再完成一件小事。"))
                    .priority(normalizePriority(textOrDefault(taskNode, "priority", "MEDIUM")))
                    .build();

            return AiSuggestResponse.builder()
                    .task(task)
                    .diaryText(textOrDefault(node, "diaryText", content))
                    .lyricInspiration(textOrDefault(node, "lyricInspiration", "雨停之後，我會慢慢走回家。"))
                    .build();
        } catch (Exception e) {
            // LLM 有時會輸出非 JSON；退回安全預設，避免整個 API 失敗
            return AiSuggestResponse.builder()
                    .task(AiSuggestResponse.SuggestedTask.builder()
                            .title("今天的一小步")
                            .description("根據你的感受：" + truncate(content, 120))
                            .priority("MEDIUM")
                            .build())
                    .diaryText(content)
                    .lyricInspiration("雨停之後，我會慢慢走回家。")
                    .build();
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        // 去掉 ```json ... ``` 或 ``` ... ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        // 若前後有說明文字，擷取第一個 { ... } 區塊
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.asText().isBlank()) {
            return defaultValue;
        }
        return value.asText().trim();
    }

    private String normalizePriority(String priority) {
        String p = priority == null ? "MEDIUM" : priority.trim().toUpperCase();
        return switch (p) {
            case "LOW", "MEDIUM", "HIGH" -> p;
            default -> "MEDIUM";
        };
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
