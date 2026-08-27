package com.nelly.navigatornest.repository;

import com.nelly.navigatornest.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    List<Diary> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Diary> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<Diary> findByUserIdAndDiaryDateOrderByCreatedAtDesc(Long userId, LocalDate diaryDate);

    List<Diary> findByUserIdAndDiaryDateBetweenOrderByDiaryDateAscCreatedAtDesc(
            Long userId, LocalDate startInclusive, LocalDate endInclusive);
}
