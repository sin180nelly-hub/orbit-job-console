package com.nelly.navigatornest.dto;

/*
 * Orbit — AI Job Execution Console
 * JobResponse：回傳給前端的 Job 資料（含狀態、結果與執行耗時）。
 */
import com.nelly.navigatornest.entity.Job.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;
    private String jobName;
    private String prompt;
    private String modelName;
    private JobStatus status;
    private String result;
    private Long executionTimeMs;
    private String ownerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
