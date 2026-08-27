package com.nelly.navigatornest.service;

/*
 * Orbit — AI Job Execution Console
 * JobService：AI Job 執行服務。
 * 負責將 PENDING 的 Job 送給本地 Ollama 推論，記錄耗時與結果，
 * 並處理模型名稱降級邏輯：前端傳入 modelName > ollama.model 配置 > llama3:8b。
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelly.navigatornest.entity.Job;
import com.nelly.navigatornest.entity.Job.JobStatus;
import com.nelly.navigatornest.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * （Orbit — AI Job Execution Console）
 * AI Job 執行服務：將 PENDING 的 Job 送給本地 Ollama 推論，並記錄耗時與結果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /** 預設模型：由 application.yml 的 ollama.model 配置，未配置時降級為 llama3:8b */
    @Value("${ollama.model:llama3:8b}")
    private String defaultModelName;

    @Transactional
    public Job save(Job job) {
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Job> getJobsByUserId(Long userId) {
        return jobRepository.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAllByOrderByIdDesc();
    }

    /** 刪除指定 Job */
    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new RuntimeException("Job not found with id: " + id);
        }
        jobRepository.deleteById(id);
    }

    /** Orbit 強制繁體中文系統提示：確保 LLM 一律回傳台灣繁體中文 */
    private static final String SYSTEM_PROMPT =
            "You are a helpful AI assistant. You MUST ALWAYS respond in Traditional Chinese (zh-TW). "
            + "Do not output English unless explicitly requested.";

    /**
     * 執行指定 Job：
     * 1. 狀態改為 RUNNING 並儲存
     * 2. 記錄開始時間
     * 3. RestTemplate POST 到本地 Ollama /api/generate（stream=false）
     * 4. 計算 executionTimeMs，結果寫回 result，狀態改為 SUCCESS／FAILED
     */
    @Transactional
    public Job executeJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));

        // 1. 標記 RUNNING
        job.setStatus(JobStatus.RUNNING);
        job = jobRepository.save(job);

        // 2. 開始計時
        long startMs = System.currentTimeMillis();

        // 模型名稱降級：前端傳入 modelName > ollama.model 配置 > llama3:8b
        String model = resolveModelName(job.getModelName());

        try {
            // 3. 呼叫本地 Ollama API
            String response = callOllamaGenerate(model, job.getPrompt());
            long executionTimeMs = System.currentTimeMillis() - startMs; // 4. 計算耗時

            job.setResult(response);
            job.setExecutionTimeMs(executionTimeMs);
            job.setStatus(JobStatus.SUCCESS);
        } catch (Exception e) {
            long executionTimeMs = System.currentTimeMillis() - startMs;
            log.error("Job {} execution failed: {}", jobId, e.getMessage(), e);

            job.setResult("Execution failed: " + e.getMessage());
            job.setExecutionTimeMs(executionTimeMs);
            job.setStatus(JobStatus.FAILED);
        }

        return jobRepository.save(job);
    }

    /**
     * （Orbit — AI Job Execution Console）
     * 模型名稱降級邏輯：
     * 1. Job 上有 modelName（前端建立時傳入）→ 直接使用
     * 2. 否則使用 application.yml 的 ollama.model 配置值
     * 3. 連配置都沒有時，最終降級為 Job.DEFAULT_MODEL_NAME（llama3:8b）
     */
    private String resolveModelName(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        if (defaultModelName != null && !defaultModelName.isBlank()) {
            return defaultModelName.trim();
        }
        return Job.DEFAULT_MODEL_NAME;
    }

    /** POST {model, prompt, stream:false} 到 http://localhost:11434/api/generate，回傳 response 文字 */
    private String callOllamaGenerate(String model, String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = ollamaBaseUrl + "/api/generate";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        // system + user：強制 LLM 回覆台灣繁體中文
        body.put("system", SYSTEM_PROMPT);
        body.put("prompt", prompt);
        body.put("stream", false);

        try {
            String raw = restTemplate.postForObject(url, body, String.class);
            if (raw == null || raw.isBlank()) {
                throw new RuntimeException("Ollama returned empty response");
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode responseNode = root.path("response");
            if (responseNode.isMissingNode() || responseNode.asText().isBlank()) {
                throw new RuntimeException("Ollama response missing 'response' field");
            }
            return responseNode.asText();
        } catch (org.springframework.web.client.RestClientException e) {
            throw new RuntimeException(
                    "Failed to call Ollama at " + url + ". Is Ollama running? " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Ollama HTTP response: " + e.getMessage(), e);
        }
    }
}
