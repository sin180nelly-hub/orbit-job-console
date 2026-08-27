package com.nelly.navigatornest.controller;

import com.nelly.navigatornest.dto.TaskRequest;
import com.nelly.navigatornest.dto.TaskResponse;
import com.nelly.navigatornest.entity.Task;
import com.nelly.navigatornest.entity.User;
import com.nelly.navigatornest.repository.UserRepository;
import com.nelly.navigatornest.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    /**
     * 從 JWT（SecurityContext）取得目前登入使用者。
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

    // 建立新任務（歸屬目前登入使用者）
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@RequestBody TaskRequest request) {
        User currentUser = getCurrentUser();

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setDueTime(request.getDueTime());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        Task createdTask = taskService.createTask(currentUser.getId(), task);

        return ResponseEntity.ok(toTaskResponse(createdTask));
    }

    /**
     * 查詢目前登入使用者的任務（使用者隔離，新到舊）。
     * <p>時間語意（優先）：
     * <ul>
     *   <li>{@code time=past|present|future}</li>
     *   <li>{@code sub=} 細項，例如 done / overdue / due_today / in_progress / next_3_days /
     *       open / scheduled / unscheduled / high</li>
     * </ul>
     * 未帶 time 時仍支援舊 query：due / hasDue / date。
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<TaskResponse>> getTasks(
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String sub,
            @RequestParam(required = false) String due,
            @RequestParam(required = false) Boolean hasDue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User currentUser = getCurrentUser();
        List<Task> tasks;
        if (time != null && !time.isBlank()) {
            tasks = taskService.getTasksByTimeSemantics(currentUser.getId(), time, sub);
        } else {
            tasks = taskService.getTasksByUserIdFiltered(
                    currentUser.getId(), due, hasDue, date);
        }

        List<TaskResponse> responseList = tasks.stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    /**
     * 月曆整月任務（僅有 dueDate 者，限目前 JWT 使用者）。
     * 例：GET /api/tasks/calendar?year=2026&month=8
     */
    @GetMapping("/calendar")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TaskResponse>> getCalendarTasks(
            @RequestParam int year,
            @RequestParam int month) {
        User currentUser = getCurrentUser();
        List<Task> tasks = taskService.getTasksForCalendarMonth(
                currentUser.getId(), year, month);
        List<TaskResponse> responseList = tasks.stream()
                .map(this::toTaskResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    // 更新任務狀態（僅能操作自己的任務）
    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(@PathVariable Long taskId,
            @RequestParam Task.TaskStatus status) {
        User currentUser = getCurrentUser();
        Task updatedTask = taskService.updateTaskStatus(taskId, currentUser.getId(), status);
        return ResponseEntity.ok(toTaskResponse(updatedTask));
    }

    /**
     * 完整更新任務（title / description / priority / status / dueDate）。
     * 僅能操作目前 JWT 使用者自己的任務。
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long taskId,
            @RequestBody TaskRequest request) {
        User currentUser = getCurrentUser();
        Task updatedTask = taskService.updateTask(taskId, currentUser.getId(), request);
        return ResponseEntity.ok(toTaskResponse(updatedTask));
    }

    // 刪除任務（僅能操作自己的任務）
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        User currentUser = getCurrentUser();
        taskService.deleteTask(taskId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    // Entity → Response（含 userId，方便前端顯示）
    private TaskResponse toTaskResponse(Task task) {
        Long ownerId = null;
        if (task.getUser() != null) {
            ownerId = task.getUser().getId();
        }
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .dueTime(task.getDueTime())
                .userId(ownerId)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
