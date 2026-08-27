package com.nelly.navigatornest.dto;

import com.nelly.navigatornest.entity.Task.TaskPriority;
import com.nelly.navigatornest.entity.Task.TaskStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TaskRequest {

    private String          title;
    private String          description;
    private TaskPriority    priority;
    private TaskStatus      status;
    private LocalDate       dueDate;
    /** 可選 HH:mm / HH:mm:ss；null 表示未設定 */
    private LocalTime       dueTime;
    /** 更新時設 true 可清空 dueTime */
    private Boolean         clearDueTime;
}