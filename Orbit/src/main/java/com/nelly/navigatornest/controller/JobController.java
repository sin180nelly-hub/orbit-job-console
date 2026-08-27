package com.nelly.navigatornest.controller;

/*
 * Orbit — AI Job Execution Console
 * JobController：AI 任務管理 API（/api/jobs）。
 * 提供建立 Job（PENDING）、查詢列表、觸發執行、單一詳情等端點，
 * 認證沿用現有 JWT（SecurityContext）機制。
 */
import com.nelly.navigatornest.dto.JobRequest;
import com.nelly.navigatornest.dto.JobResponse;
import com.nelly.navigatornest.entity.Job;
import com.nelly.navigatornest.entity.User;
import com.nelly.navigatornest.repository.UserRepository;
import com.nelly.navigatornest.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI Job 管理 API（Orbit — AI Job Execution Console）
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;

    /**
     * 從 JWT（SecurityContext）取得目前登入使用者（與 TaskController 相同機制）。
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: no authenticated user");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /** 建立新 Job（狀態 PENDING） */
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@RequestBody JobRequest request) {
        if (request.getJobName() == null || request.getJobName().isBlank()) {
            throw new IllegalArgumentException("jobName is required");
        }
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new IllegalArgumentException("prompt is required");
        }

        User currentUser = getCurrentUser();

        Job job = Job.builder()
                .jobName(request.getJobName().trim())
                .prompt(request.getPrompt())
                .modelName(request.getModelName() == null || request.getModelName().isBlank()
                        ? "llama3:8b"
                        : request.getModelName().trim())
                .build();
        job.setUser(currentUser);

        // Builder.Default 已設 PENDING；保險起見再明確指定
        job.setStatus(Job.JobStatus.PENDING);

        Job saved = jobService.save(job);
        return ResponseEntity.ok(toResponse(saved));
    }

    /** 查詢 Job 列表（目前使用者；未登入歸屬者可見全部——此 API 一律需 JWT） */
    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(@RequestParam(required = false) String status) {
        User currentUser = getCurrentUser();
        List<Job> jobs = jobService.getJobsByUserId(currentUser.getId());

        if (status != null && !status.isBlank()) {
            try {
                Job.JobStatus s = Job.JobStatus.valueOf(status.trim().toUpperCase());
                jobs = jobs.stream().filter(j -> j.getStatus() == s).toList();
            } catch (IllegalArgumentException ignored) {
                // 不合法的 status 參數就回傳全部
            }
        }
        return ResponseEntity.ok(jobs.stream().map(this::toResponse).toList());
    }

    /** 觸發執行指定 Job（同步執行：RUNNING → Ollama → SUCCESS/FAILED） */
    @PostMapping("/{id}/execute")
    public ResponseEntity<JobResponse> executeJob(@PathVariable Long id) {
        getCurrentUser(); // 確認已登入
        Job executed = jobService.executeJob(id);
        return ResponseEntity.ok(toResponse(executed));
    }

    /** 單一 Job 詳情（含 result 與耗時） */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        getCurrentUser();
        return ResponseEntity.ok(toResponse(jobService.getJobById(id)));
    }

    /** 刪除指定 Job（歷史紀錄維護） */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        getCurrentUser(); // 確認已登入
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    private JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .prompt(job.getPrompt())
                .modelName(job.getModelName())
                .status(job.getStatus())
                .result(job.getResult())
                .executionTimeMs(job.getExecutionTimeMs())
                .ownerUsername(job.getUser() != null ? job.getUser().getUsername() : null)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
