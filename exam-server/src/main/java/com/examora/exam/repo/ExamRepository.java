package com.examora.exam.repo;

import com.examora.exam.entity.Exam;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
  List<Exam> findByStatus(String status);
}
