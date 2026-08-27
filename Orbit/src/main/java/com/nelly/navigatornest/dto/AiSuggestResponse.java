package com.nelly.navigatornest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestResponse {

    private SuggestedTask task;
    private String diaryText;
    private String lyricInspiration;

    /** 當 saveToTasks=true 且成功寫入時，回傳新建任務的 id；否則為 null */
    private Long savedTaskId;

    /** 當存入日記成功時，回傳新建日記的 id；否則為 null */
    private Long savedDiaryId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedTask {
        private String title;
        private String description;
        private String priority;
    }
}
