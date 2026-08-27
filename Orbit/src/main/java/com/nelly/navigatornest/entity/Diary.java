package com.nelly.navigatornest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 可選：由 AI 建議並存任務時關聯 */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "task_id", nullable = true)
    private Task task;

    /** 使用者當下輸入原文 */
    @Column(columnDefinition = "TEXT")
    private String sourceInput;

    /** AI 產生的日記（或手動撰寫） */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String diaryText;

    /** AI 歌詞靈感（可選） */
    @Column(columnDefinition = "TEXT")
    private String lyricInspiration;

    /**
     * 日記所屬日期（日曆用；手動／AI 建立時指定或預設當天）。
     * 舊資料可能為 null，讀取時可回退 createdAt 日期。
     */
    @Column(name = "diary_date")
    private LocalDate diaryDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
