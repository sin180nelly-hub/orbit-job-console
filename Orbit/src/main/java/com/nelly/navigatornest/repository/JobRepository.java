package com.nelly.navigatornest.repository;

/*
 * Orbit — AI Job Execution Console
 * JobRepository：Job 實體的資料存取層（Spring Data JPA）。
 */
import com.nelly.navigatornest.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByUserIdOrderByIdDesc(Long userId);

    List<Job> findAllByOrderByIdDesc();
}
