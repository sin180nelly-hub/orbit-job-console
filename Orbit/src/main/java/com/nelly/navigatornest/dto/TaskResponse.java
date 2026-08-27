package com.nelly.navigatornest.dto;

import com.nelly.navigatornest.entity.Task.TaskPriority;
import com.nelly.navigatornest.entity.Task.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}