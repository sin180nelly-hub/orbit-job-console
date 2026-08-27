package com.nelly.navigatornest.entity;

/*
 * Orbit — AI Job Execution Console
 * Job Entity：代表一次送給本地 LLM（Ollama）的 AI 推論任務。
 * 由原本 Task 概念重構而來，記錄 prompt、模型、狀態、結果與執行耗時。
 */
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * AI 推論任務（Orbit — AI Job Execution Console）
 * 由原本 Task 概念重構而來：一個 Job 代表一次送給本地 LLM 的推論請求。
 */
@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任務名稱 */
    @Column(nullable = false, length = 200)
    private String jobName;

    /** 傳給 LLM 的輸入內容 */
    @Column(columnDefinition = "TEXT")
    private String prompt;

    /** 使用的模型名稱；未指定時由 Service 降級為 ollama.model 配置值 */
    @Column(length = 100)
    private String modelName;

    /** Orbit 預設模型：前端與配置檔都未提供時的最終降級值 */
    public static final String DEFAULT_MODEL_NAME = "llama3:8b";

    /** 任務狀態 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    /** LLM 回傳結果（失敗時記錄錯誤訊息） */
    @Column(columnDefinition = "TEXT")
    private String result;

    /** 執行耗時（毫秒） */
    private Long executionTimeMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum JobStatus {
        PENDING, RUNNING, SUCCESS, FAILED
    }
}
