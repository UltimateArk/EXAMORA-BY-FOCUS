package com.examora.exam.repo;

import com.examora.exam.entity.QuestionOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
  List<QuestionOption> findByQuestionId(Long questionId);
  void deleteByQuestionId(Long questionId);
}
