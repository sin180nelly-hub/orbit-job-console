package com.nelly.navigatornest.repository;

import com.nelly.navigatornest.entity.Task;
import com.nelly.navigatornest.entity.Task.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // 查詢所有任務（依 id 排序）
    List<Task> findAllByOrderByIdAsc();

    // 查詢某使用者的所有任務
    List<Task> findByUserId(Long userId);

    // 查詢某使用者的所有任務（依 id 升序）
    List<Task> findByUserIdOrderByIdAsc(Long userId);

    // 查詢某使用者的所有任務（依 id 降序：新的在前）
    List<Task> findByUserIdOrderByIdDesc(Long userId);

    // 指定截止日期（新的在前）
    List<Task> findByUserIdAndDueDateOrderByIdDesc(Long userId, LocalDate dueDate);

    // 月曆：某區間內有 dueDate 的任務（依日期、再依 id 降序）
    List<Task> findByUserIdAndDueDateBetweenOrderByDueDateAscIdDesc(
            Long userId, LocalDate startInclusive, LocalDate endInclusive);

    // 有截止日期（新的在前）
    List<Task> findByUserIdAndDueDateIsNotNullOrderByIdDesc(Long userId);

    // 無截止日期（新的在前）
    List<Task> findByUserIdAndDueDateIsNullOrderByIdDesc(Long userId);

    // 查詢某使用者的任務（分頁）
    Page<Task> findByUserId(Long userId, Pageable pageable);

    // 根據狀態查詢某使用者的任務
    List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);

    // 查詢逾期的任務（可之後擴充使用）
    List<Task> findByUserIdAndDueDateBefore(Long userId, LocalDate date);
}