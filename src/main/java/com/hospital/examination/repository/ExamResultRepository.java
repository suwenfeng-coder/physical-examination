package com.hospital.examination.repository;

import com.hospital.examination.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
}
