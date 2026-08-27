package com.nelly.navigatornest.service;

import com.nelly.navigatornest.dto.TaskRequest;
import com.nelly.navigatornest.entity.Task;
import com.nelly.navigatornest.entity.Task.TaskPriority;
import com.nelly.navigatornest.entity.Task.TaskStatus;
import com.nelly.navigatornest.entity.User;
import com.nelly.navigatornest.repository.TaskRepository;
import com.nelly.navigatornest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // 建立新任務
    @Transactional
    public Task createTask(Long userId, Task task) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        task.setUser(user);
        return taskRepository.save(task);
    }

    // 查詢所有任務（開發 / 任務列表頁用）
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return taskRepository.findAllByOrderByIdAsc();
    }

    // 查詢某使用者的所有任務（新的在前）
    @Transactional(readOnly = true)
    public List<Task> getTasksByUserId(Long userId) {
        return taskRepository.findByUserIdOrderByIdDesc(userId);
    }

    /**
     * 依截止日期條件查詢（皆限該使用者、新到舊）。
     * 優先順序：date（精確日）> due=today > hasDue true/false > 全部。
     *
     * @param due    目前支援 "today"；其他值忽略
     * @param hasDue true=有截止日、false=無截止日
     * @param date   精確 dueDate（YYYY-MM-DD）
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByUserIdFiltered(Long userId, String due, Boolean hasDue, LocalDate date) {
        if (date != null) {
            return taskRepository.findByUserIdAndDueDateOrderByIdDesc(userId, date);
        }
        if (due != null && "today".equalsIgnoreCase(due.trim())) {
            return taskRepository.findByUserIdAndDueDateOrderByIdDesc(userId, LocalDate.now());
        }
        if (Boolean.TRUE.equals(hasDue)) {
            return taskRepository.findByUserIdAndDueDateIsNotNullOrderByIdDesc(userId);
        }
        if (Boolean.FALSE.equals(hasDue)) {
            return taskRepository.findByUserIdAndDueDateIsNullOrderByIdDesc(userId);
        }
        return taskRepository.findByUserIdOrderByIdDesc(userId);
    }

    /**
     * 時間語意篩選（過去／現在／未來 + 細項），新到舊。
     * time: past | present | future
     * sub: 見 matchTimeSemantics
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByTimeSemantics(Long userId, String time, String sub) {
        LocalDate today = LocalDate.now();
        String t = time == null ? "" : time.trim().toLowerCase(Locale.ROOT);
        String s = sub == null ? "" : sub.trim().toLowerCase(Locale.ROOT);

        return taskRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(task -> matchTimeSemantics(task, t, s, today))
                .collect(Collectors.toList());
    }

    private boolean matchTimeSemantics(Task task, String time, String sub, LocalDate today) {
        TaskStatus status = task.getStatus();
        LocalDate due = task.getDueDate();
        boolean done = status == TaskStatus.DONE;
        boolean high = task.getPriority() == TaskPriority.HIGH
                || task.getPriority() == TaskPriority.URGENT;

        return switch (time) {
            case "past" -> matchPast(task, sub, today, done, due);
            case "present" -> matchPresent(task, sub, today, status, due);
            case "future" -> matchFuture(task, sub, today, done, due, high);
            default -> true;
        };
    }

    private boolean matchPast(Task task, String sub, LocalDate today, boolean done, LocalDate due) {
        // 預設：已完成 或 已逾期（未完成且 due < 今天）
        return switch (sub) {
            case "done", "completed" -> done;
            case "overdue" -> !done && due != null && due.isBefore(today);
            case "", "all" -> done || (!done && due != null && due.isBefore(today));
            default -> done || (!done && due != null && due.isBefore(today));
        };
    }

    private boolean matchPresent(Task task, String sub, LocalDate today, TaskStatus status, LocalDate due) {
        LocalDate in3 = today.plusDays(3);
        return switch (sub) {
            case "due_today", "today" -> due != null && due.equals(today);
            case "in_progress" -> status == TaskStatus.IN_PROGRESS;
            case "next_3_days", "soon" ->
                    due != null && !due.isBefore(today) && !due.isAfter(in3) && status != TaskStatus.DONE;
            case "", "all" ->
                    // 現在預設：今天到期 或 進行中 或 3 天內未完成
                    (due != null && due.equals(today))
                            || status == TaskStatus.IN_PROGRESS
                            || (due != null && !due.isBefore(today) && !due.isAfter(in3)
                            && status != TaskStatus.DONE);
            default ->
                    (due != null && due.equals(today))
                            || status == TaskStatus.IN_PROGRESS
                            || (due != null && !due.isBefore(today) && !due.isAfter(in3)
                            && status != TaskStatus.DONE);
        };
    }

    private boolean matchFuture(Task task, String sub, LocalDate today, boolean done, LocalDate due, boolean high) {
        // 未來：未完成且尚未到期（due > today 或 未排）
        boolean openNotPast = !done && (due == null || due.isAfter(today));
        return switch (sub) {
            case "open", "all_open", "all" -> openNotPast;
            case "scheduled" -> !done && due != null && due.isAfter(today);
            case "unscheduled" -> !done && due == null;
            case "high", "high_priority" -> openNotPast && high;
            case "" -> openNotPast;
            default -> openNotPast;
        };
    }

    // 分頁查詢某使用者的任務
    public Page<Task> getTasksByUserId(Long userId, Pageable pageable) {
        return taskRepository.findByUserId(userId, pageable);
    }

    /**
     * 月曆用：取得該使用者在指定年月內「有 dueDate」的任務。
     * year / month 以 1–12 的 calendar month 為準。
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksForCalendarMonth(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new RuntimeException("month must be between 1 and 12");
        }
        if (year < 1970 || year > 2100) {
            throw new RuntimeException("year out of supported range");
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return taskRepository.findByUserIdAndDueDateBetweenOrderByDueDateAscIdDesc(
                userId, start, end);
    }

    // 更新任務狀態
    @Transactional
    public Task updateTaskStatus(Long taskId, Long userId, TaskStatus newStatus) {
        Task task = findOwnedTask(taskId, userId);
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    /**
     * 完整更新任務欄位（僅限擁有者）。
     * title 必填；其餘依 request 更新（dueDate 可為 null 表示清空）。
     */
    @Transactional
    public Task updateTask(Long taskId, Long userId, TaskRequest request) {
        Task task = findOwnedTask(taskId, userId);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("title must not be blank");
        }

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        // dueDate：request 有帶此欄位時更新（含 null 清空）
        task.setDueDate(request.getDueDate());

        if (Boolean.TRUE.equals(request.getClearDueTime())) {
            task.setDueTime(null);
        } else if (request.getDueTime() != null) {
            task.setDueTime(request.getDueTime());
        }
        // 若清空 dueDate，一併清空 dueTime
        if (request.getDueDate() == null) {
            task.setDueTime(null);
        }

        return taskRepository.save(task);
    }

    // 刪除任務
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = findOwnedTask(taskId, userId);
        taskRepository.delete(task);
    }

    private Task findOwnedTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));

        if (task.getUser() == null || !task.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to modify this task");
        }
        return task;
    }
}