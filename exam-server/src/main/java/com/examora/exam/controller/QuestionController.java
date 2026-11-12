package com.examora.exam.controller;

import com.examora.exam.dto.SavedQuestionDto;
import com.examora.exam.service.QuestionService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

  private final QuestionService service;

  public QuestionController(QuestionService service) {
    this.service = service;
  }

  @GetMapping("/paper/{paperId}")
  public List<SavedQuestionDto> byPaper(@PathVariable Long paperId) {
    return service.listByPaper(paperId);
  }

  @GetMapping("/generate")
  public List<SavedQuestionDto> generate(
      @RequestParam(required = false) Long subjectId,
      @RequestParam(required = false) Long topicId,
      @RequestParam(required = false) String difficulty,
      @RequestParam(required = false) String examType,
      @RequestParam(required = false, defaultValue = "10") int count) {
    return service.generate(subjectId, topicId, difficulty, examType, count);
  }

  @PatchMapping("/{id}/verify")
  public SavedQuestionDto verify(@PathVariable Long id,
      @RequestParam String status) {
    return service.verify(id, status);
  }

  @PutMapping("/{id}")
  public SavedQuestionDto update(@PathVariable Long id,
      @RequestBody Map<String, Object> body) {
    return service.update(id, body);
  }

  @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SavedQuestionDto replaceImage(@PathVariable Long id,
      @RequestParam("file") MultipartFile file) {
    return service.replaceImage(id, file);
  }

  @PostMapping(value = "/{qid}/options/{label}/image",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SavedQuestionDto replaceOptionImage(@PathVariable Long qid,
      @PathVariable String label,
      @RequestParam("file") MultipartFile file) {
    return service.replaceOptionImage(qid, label, file);
  }

  @DeleteMapping("/{id}/image")
  public SavedQuestionDto clearImage(@PathVariable Long id) {
    return service.clearImage(id);
  }
}
