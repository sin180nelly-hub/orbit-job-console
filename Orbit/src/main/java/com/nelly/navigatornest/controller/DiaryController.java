package com.nelly.navigatornest.controller;

import com.nelly.navigatornest.dto.DiaryRequest;
import com.nelly.navigatornest.dto.DiaryResponse;
import com.nelly.navigatornest.entity.Diary;
import com.nelly.navigatornest.entity.User;
import com.nelly.navigatornest.repository.UserRepository;
import com.nelly.navigatornest.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: no authenticated user");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * 目前使用者的日記列表（新到舊）。
     * 可選 {@code date=YYYY-MM-DD} 只取該日曆日。
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<DiaryResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = getCurrentUser();
        List<Diary> diaries = (date != null)
                ? diaryService.listByUserIdAndDate(user.getId(), date)
                : diaryService.listByUserId(user.getId());
        List<DiaryResponse> list = diaries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * 月曆整月日記。例：GET /api/diaries/calendar?year=2026&month=8
     */
    @GetMapping("/calendar")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DiaryResponse>> calendarMonth(
            @RequestParam int year,
            @RequestParam int month) {
        User user = getCurrentUser();
        List<DiaryResponse> list = diaryService.listForCalendarMonth(user.getId(), year, month).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<DiaryResponse> get(@PathVariable Long id) {
        User user = getCurrentUser();
        Diary diary = diaryService.getByIdForUser(id, user.getId());
        return ResponseEntity.ok(toResponse(diary));
    }

    /** 手動新增日記 */
    @PostMapping
    public ResponseEntity<DiaryResponse> create(@RequestBody DiaryRequest request) {
        User user = getCurrentUser();
        Diary created = diaryService.create(user.getId(), request);
        return ResponseEntity.ok(toResponse(created));
    }

    /**
     * 更新日記（可補關聯 taskId；clearTask=true 解除關聯）。
     */
    @PutMapping("/{id}")
    public ResponseEntity<DiaryResponse> update(@PathVariable Long id,
            @RequestBody DiaryRequest request) {
        User user = getCurrentUser();
        Diary updated = diaryService.update(id, user.getId(), request);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User user = getCurrentUser();
        diaryService.deleteForUser(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private DiaryResponse toResponse(Diary diary) {
        Long userId = diary.getUser() != null ? diary.getUser().getId() : null;
        Long taskId = null;
        String taskTitle = null;
        if (diary.getTask() != null) {
            taskId = diary.getTask().getId();
            taskTitle = diary.getTask().getTitle();
        }
        LocalDate diaryDate = DiaryService.effectiveDate(diary);
        return DiaryResponse.builder()
                .id(diary.getId())
                .userId(userId)
                .taskId(taskId)
                .taskTitle(taskTitle)
                .sourceInput(diary.getSourceInput())
                .diaryText(diary.getDiaryText())
                .lyricInspiration(diary.getLyricInspiration())
                .diaryDate(diaryDate)
                .createdAt(diary.getCreatedAt())
                .build();
    }
}
