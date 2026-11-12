package com.examora.exam.repo;

import com.examora.exam.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
  List<Question> findByPaperId(Long paperId);

  /** Bulk delete scoped to one paper (used for idempotent re-runs of extraction). */
  @Modifying
  @Query("DELETE FROM Question q WHERE q.paper.id = :paperId")
  void deleteByPaperId(@Param("paperId") Long paperId);

  List<Question> findBySubjectIdAndTopicIdAndDifficultyAndExamType(
      Long subjectId, Long topicId, Question.Difficulty difficulty, String examType);

  @Query(value = "SELECT * FROM questions q WHERE "
      + "(:query IS NULL OR q.question_text ILIKE '%' || :query || '%' "
      + " OR q.markdown ILIKE '%' || :query || '%' "
      + " OR q.latex ILIKE '%' || :query || '%') AND "
      + "(:subjectId IS NULL OR q.subject_id = :subjectId) AND "
      + "(:topicId IS NULL OR q.topic_id = :topicId) AND "
      + "(:subtopicId IS NULL OR q.subtopic_id = :subtopicId) AND "
      + "(:difficulty IS NULL OR q.difficulty = :difficulty) AND "
      + "(:verificationStatus IS NULL OR q.verification_status = :verificationStatus) AND "
      + "(:examType IS NULL OR q.exam_type = :examType) "
      + "ORDER BY q.id DESC LIMIT :limit", nativeQuery = true)
  List<Question> search(@Param("query") String query,
      @Param("subjectId") Long subjectId,
      @Param("topicId") Long topicId,
      @Param("subtopicId") Long subtopicId,
      @Param("difficulty") String difficulty,
      @Param("verificationStatus") String verificationStatus,
      @Param("examType") String examType,
      @Param("limit") int limit);

  @Query(value = "SELECT * FROM questions q WHERE "
      + "(:subjectId IS NULL OR q.subject_id = :subjectId) AND "
      + "(:topicId IS NULL OR q.topic_id = :topicId) AND "
      + "(:difficulty IS NULL OR q.difficulty = :difficulty) AND "
      + "(:examType IS NULL OR q.exam_type = :examType) AND "
      + "q.verification_status = 'APPROVED' "
      + "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
  List<Question> findRandomByFilters(@Param("subjectId") Long subjectId,
      @Param("topicId") Long topicId,
      @Param("difficulty") String difficulty,
      @Param("examType") String examType,
      @Param("limit") int limit);
}
