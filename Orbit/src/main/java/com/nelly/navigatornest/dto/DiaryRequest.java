package com.nelly.navigatornest.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DiaryRequest {

    /** 使用者原文（可選） */
    private String sourceInput;

    /** 日記本文（必填） */
    private String diaryText;

    /** 歌詞靈感（可選） */
    private String lyricInspiration;

    /** 可選關聯任務 id（須為目前使用者擁有） */
    private Long taskId;

    /** 日記所屬日期（日曆用；省略則用今天） */
    private LocalDate diaryDate;

    /**
     * 更新時：true 表示取消關聯任務。
     * 與 taskId 同時存在時，clearTask 優先。
     */
    private Boolean clearTask;
}
