package com.nelly.navigatornest.dto;

import lombok.Data;

@Data
public class AiSuggestRequest {

    private String userInput;

    /** 若為 true，會將 AI 產生的 task 存入資料庫，並同步存一筆 Diary（關聯該 Task） */
    private boolean saveToTasks = false;

    /**
     * 若為 true，將 AI 日記／歌詞存成獨立 Diary。
     * 可單獨使用（只存日記）；若同時 saveToTasks=true，日記會關聯新建任務。
     */
    private boolean saveToDiary = false;
}
