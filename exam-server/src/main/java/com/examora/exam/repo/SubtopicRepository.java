package com.examora.exam.repo;

import com.examora.exam.entity.Subtopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubtopicRepository extends JpaRepository<Subtopic, Long> {
  List<Subtopic> findByTopicId(Long topicId);
}
