package com.examora.exam.controller;

import com.examora.exam.entity.Subject;
import com.examora.exam.entity.Topic;
import com.examora.exam.repo.SubjectRepository;
import com.examora.exam.repo.TopicRepository;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class SubjectController {

  private final SubjectRepository subjects;
  private final TopicRepository topics;

  public SubjectController(SubjectRepository subjects, TopicRepository topics) {
    this.subjects = subjects;
    this.topics = topics;
  }

  @GetMapping("/api/subjects")
  public List<Subject> allSubjects() {
    return subjects.findAll();
  }

  @GetMapping("/api/topics/subject/{subjectId}")
  public List<Topic> bySubject(@PathVariable Long subjectId) {
    return topics.findBySubjectId(subjectId);
  }

  @PostMapping("/api/subjects/seed")
  public List<Subject> seedSubjects(@RequestBody(required = false) List<Map<String, String>> body) {
    if (body == null || body.isEmpty()) {
      body = List.of(Map.of("name", "Physics"), Map.of("name", "Chemistry"),
          Map.of("name", "Mathematics"));
    }
    for (Map<String, String> row : body) {
      String name = row.get("name");
      boolean exists = subjects.findAll().stream().anyMatch(s -> s.getName().equalsIgnoreCase(name));
      if (!exists) subjects.save(new Subject(name));
    }
    return subjects.findAll();
  }

  @PostMapping("/api/topics/seed")
  public List<Topic> seedTopics(@RequestBody(required = false) List<Map<String, String>> body) {
    if (body == null) body = List.of();
    for (Map<String, String> row : body) {
      Topic t = new Topic();
      t.setName(row.get("name"));
      t.setSyllabusType(row.getOrDefault("syllabusType", "JEE"));
      Long sid = Long.valueOf(row.get("subjectId"));
      Subject s = subjects.findById(sid).orElseThrow();
      t.setSubject(s);
      topics.save(t);
    }
    return topics.findAll();
  }
}
