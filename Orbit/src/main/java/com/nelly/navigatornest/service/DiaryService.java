package com.nelly.navigatornest.service;

import com.nelly.navigatornest.dto.DiaryRequest;
import com.nelly.navigatornest.entity.Diary;
import com.nelly.navigatornest.entity.Task;
import com.nelly.navigatornest.entity.User;
import com.nelly.navigatornest.repository.DiaryRepository;
import com.nelly.navigatornest.repository.TaskRepository;
import com.nelly.navigatornest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    /**
     * 全部日記：優先 diaryDate 新到舊，同日再依 createdAt 新到舊。
     * diaryDate 為 null 時以 createdAt 日期回退。
     */
    @Transactional(readOnly = true)
    public List<Diary> listByUserId(Long userId) {
        List<Diary> list = new ArrayList<>(diaryRepository.findByUserIdOrderByCreatedAtDesc(userId));
        list.sort(Comparator
                .comparing((Diary d) -> effectiveDate(d), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Diary::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    @Transactional(readOnly = true)
    public Diary getByIdForUser(Long diaryId, Long userId) {
        return diaryRepository.findByIdAndUserId(diaryId, userId)
                .orElseThrow(() -> new RuntimeException("Diary not found with id: " + diaryId));
    }

    /**
     * 指定日曆日的日記（含 diaryDate 符合，或舊資料 diaryDate 為 null 且 createdAt 日期符合）。
     */
    @Transactional(readOnly = true)
    public List<Diary> listByUserIdAndDate(Long userId, LocalDate date) {
        if (date == null) {
            throw new RuntimeException("date is required");
        }
        Map<Long, Diary> map = new LinkedHashMap<>();
        for (Diary d : diaryRepository.findByUserIdAndDiaryDateOrderByCreatedAtDesc(userId, date)) {
            map.put(d.getId(), d);
        }
        for (Diary d : diaryRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (d.getDiaryDate() == null && d.getCreatedAt() != null
                    && d.getCreatedAt().toLocalDate().equals(date)) {
                map.putIfAbsent(d.getId(), d);
            }
        }
        List<Diary> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparing(Diary::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    /**
     * 月曆整月日記（diaryDate 在區間內；並合併舊資料以 createdAt 落在該月者）。
     */
    @Transactional(readOnly = true)
    public List<Diary> listForCalendarMonth(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new RuntimeException("month must be between 1 and 12");
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        Map<Long, Diary> map = new LinkedHashMap<>();
        for (Diary d : diaryRepository.findByUserIdAndDiaryDateBetweenOrderByDiaryDateAscCreatedAtDesc(
                userId, start, end)) {
            map.put(d.getId(), d);
        }
        for (Diary d : diaryRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (d.getDiaryDate() == null && d.getCreatedAt() != null) {
                LocalDate cd = d.getCreatedAt().toLocalDate();
                if (!cd.isBefore(start) && !cd.isAfter(end)) {
                    map.putIfAbsent(d.getId(), d);
                }
            }
        }
        List<Diary> list = new ArrayList<>(map.values());
        list.sort(Comparator
                .comparing((Diary d) -> effectiveDate(d), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Diary::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    public static LocalDate effectiveDate(Diary d) {
        if (d.getDiaryDate() != null) {
            return d.getDiaryDate();
        }
        if (d.getCreatedAt() != null) {
            return d.getCreatedAt().toLocalDate();
        }
        return null;
    }

    /**
     * 手動新增日記（可選關聯自己的 task、指定 diaryDate）。
     */
    @Transactional
    public Diary create(Long userId, DiaryRequest request) {
        if (request.getDiaryText() == null || request.getDiaryText().isBlank()) {
            throw new RuntimeException("diaryText must not be blank");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Task task = null;
        if (request.getTaskId() != null) {
            task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + request.getTaskId()));
            if (task.getUser() == null || !task.getUser().getId().equals(userId)) {
                throw new RuntimeException("You don't have permission to link this task");
            }
        }

        LocalDate diaryDate = request.getDiaryDate() != null ? request.getDiaryDate() : LocalDate.now();

        Diary diary = Diary.builder()
                .user(user)
                .task(task)
                .sourceInput(request.getSourceInput())
                .diaryText(request.getDiaryText().trim())
                .lyricInspiration(request.getLyricInspiration())
                .diaryDate(diaryDate)
                .build();

        return diaryRepository.save(diary);
    }

    /**
     * AI 流程用：建立日記並可選關聯剛建立的任務（diaryDate = 今天）。
     */
    @Transactional
    public Diary createFromAi(Long userId, Long taskId, String sourceInput,
            String diaryText, String lyricInspiration) {
        if (diaryText == null || diaryText.isBlank()) {
            diaryText = "（無日記內容）";
        }

        DiaryRequest request = new DiaryRequest();
        request.setSourceInput(sourceInput);
        request.setDiaryText(diaryText);
        request.setLyricInspiration(lyricInspiration);
        request.setTaskId(taskId);
        request.setDiaryDate(LocalDate.now());
        return create(userId, request);
    }

    /**
     * 更新日記（擁有者）：可改本文／原文／歌詞／diaryDate／關聯 task（taskId null 表示取消關聯）。
     */
    @Transactional
    public Diary update(Long diaryId, Long userId, DiaryRequest request) {
        Diary diary = getByIdForUser(diaryId, userId);

        if (request.getDiaryText() != null) {
            if (request.getDiaryText().isBlank()) {
                throw new RuntimeException("diaryText must not be blank");
            }
            diary.setDiaryText(request.getDiaryText().trim());
        }
        if (request.getSourceInput() != null) {
            diary.setSourceInput(request.getSourceInput());
        }
        if (request.getLyricInspiration() != null) {
            diary.setLyricInspiration(request.getLyricInspiration());
        }
        if (request.getDiaryDate() != null) {
            diary.setDiaryDate(request.getDiaryDate());
        }

        // taskId：欄位有出現在 JSON 時才更新；用 null 可解除關聯
        // 由 controller 傳 clearTask 旗標較明確——此處：若 request 含 taskId 概念
        // 簡化：request.getTaskId() 非 null 則綁定；若特別要解綁，傳 taskId 不在 body 用 clearTask
        // 為支援「可空」，用 Optional 風格：DiaryRequest 增設 Boolean clearTask 太重
        // 約定：update 時若 body 有 "taskId" key——用 wrapper
        // 實作：若 taskId != null 綁定；若 clearTaskAssociation=true 解綁
        if (Boolean.TRUE.equals(request.getClearTask())) {
            diary.setTask(null);
        } else if (request.getTaskId() != null) {
            Task task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + request.getTaskId()));
            if (task.getUser() == null || !task.getUser().getId().equals(userId)) {
                throw new RuntimeException("You don't have permission to link this task");
            }
            diary.setTask(task);
        }

        return diaryRepository.save(diary);
    }

    @Transactional
    public void deleteForUser(Long diaryId, Long userId) {
        Diary diary = getByIdForUser(diaryId, userId);
        diaryRepository.delete(diary);
    }
}
