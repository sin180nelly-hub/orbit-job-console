package com.nelly.navigatornest.controller;

/*
 * Orbit — AI Job Execution Console
 * HealthController：輕量服務健康檢查端點。
 * 前端定時呼叫 GET /api/health，由後端探測本地 Ollama 是否運行，
 * 避免瀏覽器直接跨網域打 Ollama (CORS 問題)。
 */
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
public class HealthController {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Ollama 離線時的 Fallback 模型清單 */
    private static final List<String> FALLBACK_MODELS =
            List.of("llama3:8b", "taide-b5-7b", "code-llama:7b");

    /**
     * 回傳 LLM 服務狀態：
     * { "ollama": "UP" | "DOWN", "baseUrl": "http://localhost:11434" }
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean up = pingOllama();
        return ResponseEntity.ok(Map.of(
                "ollama", up ? "UP" : "DOWN",
                "baseUrl", ollamaBaseUrl
        ));
    }

    /** 探測 Ollama /api/tags（2 秒逾時，不打擾主流程） */
    private boolean pingOllama() {
        try {
            RestTemplate rt = new RestTemplate();
            rt.getForObject(ollamaBaseUrl + "/api/tags", String.class);
            return true;
        } catch (Exception e) {
            log.debug("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 動態模型清單：向 Ollama GET /api/tags 取得已安裝模型（name 欄位）。
     * Ollama 離線或解析失敗時，回傳 Fallback 預設清單。
     * 回應格式：{ "models": ["llama3:8b", ...], "source": "ollama" | "fallback" }
     */
    @GetMapping("/api/models")
    public ResponseEntity<Map<String, Object>> models() {
        List<String> models = fetchModelsFromOllama();
        return ResponseEntity.ok(Map.of(
                "models", models,
                "source", models.equals(FALLBACK_MODELS) ? "fallback" : "ollama"
        ));
    }

    /** 解析 Ollama /api/tags 的 models[].name；失敗時降級為預設清單 */
    private List<String> fetchModelsFromOllama() {
        try {
            RestTemplate rt = new RestTemplate();
            String raw = rt.getForObject(ollamaBaseUrl + "/api/tags", String.class);
            if (raw == null || raw.isBlank()) {
                log.warn("Ollama /api/tags returned empty, using fallback model list");
                return FALLBACK_MODELS;
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode modelsNode = root.path("models");
            if (!modelsNode.isArray() || modelsNode.isEmpty()) {
                return FALLBACK_MODELS;
            }

            List<String> names = new ArrayList<>();
            for (JsonNode m : modelsNode) {
                String name = m.path("name").asText(null);
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
            return names.isEmpty() ? FALLBACK_MODELS : names;
        } catch (Exception e) {
            log.warn("Failed to fetch models from Ollama, using fallback list: {}", e.getMessage());
            return FALLBACK_MODELS;
        }
    }
}
