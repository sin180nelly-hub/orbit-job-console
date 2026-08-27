package com.nelly.navigatornest.dto;

/*
 * Orbit — AI Job Execution Console
 * JobRequest：建立 Job 時的請求 Body（jobName、prompt 必填，modelName 可選）。
 */
import lombok.Data;

@Data
public class JobRequest {

    private String jobName;

    /** 傳給 LLM 的輸入內容 */
    private String prompt;

    /** 可選；未提供時預設 llama3:8b */
    private String modelName;
}
