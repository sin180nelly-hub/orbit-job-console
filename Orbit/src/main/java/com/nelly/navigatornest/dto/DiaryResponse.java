package com.nelly.navigatornest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryResponse {

    private Long id;
    private Long userId;
    /** 關聯任務 id（可 null） */
    private Long taskId;
    /** 關聯任務標題（方便日曆／列表顯示） */
    private String taskTitle;
    private String sourceInput;
    private String diaryText;
    private String lyricInspiration;
    /** 日曆對應日期（null 時前端可回退 createdAt 日期） */
    private LocalDate diaryDate;
    private LocalDateTime createdAt;
}
