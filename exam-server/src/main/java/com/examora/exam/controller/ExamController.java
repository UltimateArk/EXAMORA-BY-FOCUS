package com.examora.exam.controller;

import com.examora.exam.dto.ExamRequest;
import com.examora.exam.dto.ExamResponse;
import com.examora.exam.dto.SavedQuestionDto;
import com.examora.exam.service.ExamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

  private final ExamService service;

  public ExamController(ExamService service) {
    this.service = service;
  }

  public record PaperAttachRequest(Long paperId) {}
  public record QuestionAttachRequest(List<Long> questionIds) {}

  @PostMapping
  public ResponseEntity<ExamResponse> create(@Valid @RequestBody ExamRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
  }

  @GetMapping
  public List<ExamResponse> list(@RequestParam(required = false) String status) {
    return service.list(status);
  }

  @GetMapping("/upcoming")
  public List<ExamResponse> upcoming() {
    return service.upcoming();
  }

  @GetMapping("/{id}")
  public ExamResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PutMapping("/{id}")
  public ExamResponse update(@PathVariable Long id, @Valid @RequestBody ExamRequest req) {
    return service.update(id, req);
  }

  @PostMapping("/{id}/publish")
  public ExamResponse publish(@PathVariable Long id) {
    return service.publish(id);
  }

  @PostMapping("/{id}/papers")
  public Map<String, Object> attachPaper(@PathVariable Long id,
      @RequestBody PaperAttachRequest body) {
    int added = service.attachPaper(id, body.paperId());
    return Map.of("examId", id, "added", added);
  }

  @PostMapping("/{id}/questions")
  public Map<String, Object> attachQuestions(@PathVariable Long id,
      @RequestBody QuestionAttachRequest body) {
    int added = service.attachQuestions(id, body.questionIds());
    return Map.of("examId", id, "added", added);
  }

  @GetMapping("/{id}/questions")
  public List<SavedQuestionDto> listQuestions(@PathVariable Long id) {
    return service.listQuestions(id);
  }

  @DeleteMapping("/{id}/questions/{questionId}")
  public ResponseEntity<Void> detachQuestion(@PathVariable Long id,
      @PathVariable Long questionId) {
    service.detachQuestion(id, questionId);
    return ResponseEntity.noContent().build();
  }
}
