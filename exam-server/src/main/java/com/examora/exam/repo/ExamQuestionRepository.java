package com.examora.exam.repo;

import com.examora.exam.entity.ExamQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
  List<ExamQuestion> findByExamIdOrderByQuestionOrderAsc(Long examId);

  void deleteByExamIdAndQuestionId(Long examId, Long questionId);

  boolean existsByExamIdAndQuestionId(Long examId, Long questionId);
}
