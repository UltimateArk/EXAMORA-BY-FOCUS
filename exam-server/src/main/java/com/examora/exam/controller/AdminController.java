package com.examora.exam.controller;

import com.examora.exam.dto.ExtractionResponse;
import com.examora.exam.dto.PageImageDto;
import com.examora.exam.dto.PageText;
import com.examora.exam.dto.QuestionRegionDto;
import com.examora.exam.dto.QuestionSaveResponse;
import com.examora.exam.dto.UploadResponse;
import com.examora.exam.entity.Question;
import com.examora.exam.entity.QuestionPaper;
import com.examora.exam.repo.QuestionPaperRepository;
import com.examora.exam.repo.QuestionRepository;
import com.examora.exam.service.ExtractionService;
import com.examora.exam.service.PdfImageService;
import com.examora.exam.service.StorageService;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final QuestionPaperRepository papers;
  private final QuestionRepository questions;
  private final StorageService storage;
  private final ExtractionService extraction;

  public AdminController(QuestionPaperRepository papers, QuestionRepository questions,
      StorageService storage, ExtractionService extraction) {
    this.papers = papers;
    this.questions = questions;
    this.storage = storage;
    this.extraction = extraction;
  }

  @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UploadResponse uploadPdf(@RequestParam("file") MultipartFile file) throws Exception {
    QuestionPaper paper = new QuestionPaper();
    paper.setFileName(file.getOriginalFilename());
    paper.setStoredPath("");
    paper.setStatus(QuestionPaper.Status.UPLOADED);
    paper = papers.save(paper);
    Path dest = storage.saveUpload(file, paper.getId());
    paper.setStoredPath(dest.toString());
    papers.save(paper);
    return new UploadResponse(paper.getId(), paper.getFileName(), "uploaded");
  }

  @GetMapping("/extract-text/{paperId}")
  public ExtractionResponse extractText(@PathVariable Long paperId) {
    QuestionPaper paper = papers.findById(paperId).orElseThrow();
    List<PageText> pages = extraction.extractText(paperId);
    return new ExtractionResponse(paperId, paper.getFileName(), pages, List.of(), List.of());
  }

  @GetMapping("/extract-question-images/{paperId}")
  public Map<String, Object> extractQuestionImages(@PathVariable Long paperId) throws Exception {
    QuestionPaper paper = papers.findById(paperId).orElseThrow();
    PdfImageService.RenderResult r = extraction.extractImages(paperId);
    List<PageImageDto> images = r.images();
    List<QuestionRegionDto> questions = r.questions();
    return Map.of("paperId", paperId, "fileName", paper.getFileName(),
        "images", images, "questions", questions);
  }

  @PostMapping("/save-extracted-questions/{paperId}")
  public QuestionSaveResponse saveExtracted(@PathVariable Long paperId,
      @RequestParam(required = false) String examType,
      @RequestParam(required = false) Long subjectId,
      @RequestParam(required = false) Long topicId,
      @RequestParam(required = false, defaultValue = "MEDIUM") String difficulty) {
    int n = extraction.saveExtracted(paperId, examType, subjectId, topicId, difficulty);
    return new QuestionSaveResponse(n);
  }

  @PostMapping("/process-paper/{paperId}")
  public Map<String, Object> processPaper(@PathVariable Long paperId,
      @RequestParam(required = false) String examType,
      @RequestParam(required = false) Long subjectId,
      @RequestParam(required = false) Long topicId,
      @RequestParam(required = false, defaultValue = "MEDIUM") String difficulty) {
    return extraction.processPaper(paperId, examType, subjectId, topicId, difficulty);
  }

  @PostMapping("/papers/{paperId}/submit")
  public ResponseEntity<Map<String, Object>> submitPaper(@PathVariable Long paperId) {
    QuestionPaper paper = papers.findById(paperId).orElseThrow();
    List<Question> qs = questions.findByPaperId(paperId);
    long pending = qs.stream()
        .filter(q -> q.getVerificationStatus() == Question.VerificationStatus.PENDING)
        .count();
    if (pending > 0) {
      return ResponseEntity.status(409).body(Map.of("paperId", paperId, "pending", pending));
    }
    paper.setStatus(QuestionPaper.Status.SUBMITTED);
    papers.save(paper);
    return ResponseEntity.ok(Map.of("paperId", paperId, "submitted", qs.size()));
  }

  @GetMapping("/images/{paperId}/page-{page}.png")
  public ResponseEntity<Resource> pageImage(@PathVariable Long paperId,
      @PathVariable int page) throws Exception {
    Path p = storage.resolveSecure(paperId, "page-" + page + ".png");
    Resource r = storage.loadAsResource(p);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(r);
  }

  @GetMapping("/question-images/{paperId}/{file}")
  public ResponseEntity<Resource> questionImage(@PathVariable Long paperId,
      @PathVariable String file) throws Exception {
    Path p = storage.resolveSecure(paperId, file);
    Resource r = storage.loadAsResource(p);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(r);
  }
}
