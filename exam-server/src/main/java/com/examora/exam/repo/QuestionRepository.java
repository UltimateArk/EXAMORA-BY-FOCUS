package com.examora.exam.repo;

import com.examora.exam.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
  List<Question> findByPaperId(Long paperId);

  List<Question> findBySubjectIdAndTopicIdAndDifficultyAndExamType(
      Long subjectId, Long topicId, Question.Difficulty difficulty, String examType);

  @Query(value = "SELECT * FROM questions q WHERE "
      + "(:subjectId IS NULL OR q.subject_id = :subjectId) AND "
      + "(:topicId IS NULL OR q.topic_id = :topicId) AND "
      + "(:difficulty IS NULL OR q.difficulty = :difficulty) AND "
      + "(:examType IS NULL OR q.exam_type = :examType) "
      + "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
  List<Question> findRandomByFilters(@Param("subjectId") Long subjectId,
      @Param("topicId") Long topicId,
      @Param("difficulty") String difficulty,
      @Param("examType") String examType,
      @Param("limit") int limit);
}
