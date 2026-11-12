package com.examora.exam.service;

import com.examora.exam.dto.ExamRequest;
import com.examora.exam.dto.ExamResponse;
import com.examora.exam.dto.SavedQuestionDto;
import com.examora.exam.entity.Exam;
import com.examora.exam.entity.ExamQuestion;
import com.examora.exam.entity.Question;
import com.examora.exam.repo.ExamQuestionRepository;
import com.examora.exam.repo.ExamRepository;
import com.examora.exam.repo.QuestionOptionRepository;
import com.examora.exam.repo.QuestionPaperRepository;
import com.examora.exam.repo.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamService {

  private final ExamRepository exams;
  private final ExamQuestionRepository examQuestions;
  private final QuestionRepository questions;
  private final QuestionPaperRepository papers;
  private final QuestionOptionRepository options;
  private final QuestionService questionService;
  private final ObjectMapper objectMapper;

  public ExamService(ExamRepository exams, ExamQuestionRepository examQuestions,
      QuestionRepository questions, QuestionPaperRepository papers,
      QuestionOptionRepository options, QuestionService questionService,
      ObjectMapper objectMapper) {
    this.exams = exams;
    this.examQuestions = examQuestions;
    this.questions = questions;
    this.papers = papers;
    this.options = options;
    this.questionService = questionService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public ExamResponse create(ExamRequest req) {
    validateTimes(req.startTime(), req.endTime(), req.durationMinutes());
    Exam e = new Exam();
    apply(e, req);
    if (e.getStatus() == null) e.setStatus("DRAFT");
    return ExamResponse.from(exams.save(e));
  }

  @Transactional(readOnly = true)
  public List<ExamResponse> list(String status) {
    List<Exam> found = (status == null || status.isBlank())
        ? exams.findAll()
        : exams.findByStatus(status.toUpperCase());
    return found.stream().map(ExamResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<ExamResponse> upcoming() {
    LocalDate today = LocalDate.now();
    return exams.findAll().stream()
        .filter(e -> "SCHEDULED".equals(e.getStatus()) || "PUBLISHED".equals(e.getStatus()))
        .filter(e -> e.getExamDate() == null || !e.getExamDate().isBefore(today))
        .sorted(Comparator.comparing(Exam::getExamDate,
            Comparator.nullsLast(Comparator.naturalOrder())))
        .map(ExamResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public ExamResponse get(Long id) {
    return ExamResponse.from(findOr404(id));
  }

  @Transactional
  public ExamResponse update(Long id, ExamRequest req) {
    Exam e = findOr404(id);
    requireEditable(e);
    validateTimes(req.startTime(), req.endTime(), req.durationMinutes());
    apply(e, req);
    return ExamResponse.from(exams.save(e));
  }

  @Transactional
  public ExamResponse publish(Long id) {
    Exam e = findOr404(id);
    if (!"DRAFT".equals(e.getStatus()) && !"SCHEDULED".equals(e.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Only DRAFT/SCHEDULED exams can be published");
    }
    e.setStatus("PUBLISHED");
    return ExamResponse.from(exams.save(e));
  }

  @Transactional
  public int attachPaper(Long examId, Long paperId) {
    Exam exam = findOr404(examId);
    requireEditable(exam);
    papers.findById(paperId).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found: " + paperId));
    List<Question> paperQuestions = new ArrayList<>(questions.findByPaperId(paperId));
    List<Question> approved = paperQuestions.stream()
        .filter(q -> q.getVerificationStatus() == Question.VerificationStatus.APPROVED)
        .toList();
    List<Question> toCopy = approved.isEmpty() ? paperQuestions : approved;
    toCopy.sort(Comparator.comparing(Question::getQuestionNumber,
        Comparator.nullsLast(Comparator.naturalOrder())));
    int order = nextOrder(examId);
    int added = 0;
    for (Question q : toCopy) {
      if (examQuestions.existsByExamIdAndQuestionId(examId, q.getId())) continue;
      ExamQuestion eq = new ExamQuestion();
      eq.setExam(exam);
      eq.setQuestion(q);
      eq.setQuestionOrder(order++);
      eq.setSnapshotJson(snapshot(q));
      examQuestions.save(eq);
      added++;
    }
    return added;
  }

  @Transactional
  public int attachQuestions(Long examId, List<Long> questionIds) {
    Exam exam = findOr404(examId);
    requireEditable(exam);
    if (questionIds == null || questionIds.isEmpty()) return 0;
    int order = nextOrder(examId);
    int added = 0;
    for (Long qid : questionIds) {
      Question q = questions.findById(qid).orElseThrow(() ->
          new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + qid));
      if (examQuestions.existsByExamIdAndQuestionId(examId, qid)) continue;
      ExamQuestion eq = new ExamQuestion();
      eq.setExam(exam);
      eq.setQuestion(q);
      eq.setQuestionOrder(order++);
      eq.setSnapshotJson(snapshot(q));
      examQuestions.save(eq);
      added++;
    }
    return added;
  }

  @Transactional(readOnly = true)
  public List<SavedQuestionDto> listQuestions(Long examId) {
    findOr404(examId);
    return examQuestions.findByExamIdOrderByQuestionOrderAsc(examId).stream()
        .map(eq -> questionService.toDto(eq.getQuestion()))
        .toList();
  }

  @Transactional
  public void detachQuestion(Long examId, Long questionId) {
    findOr404(examId);
    if (!examQuestions.existsByExamIdAndQuestionId(examId, questionId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not attached to exam");
    }
    examQuestions.deleteByExamIdAndQuestionId(examId, questionId);
  }

  private Exam findOr404(Long id) {
    return exams.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found: " + id));
  }

  private void requireEditable(Exam e) {
    if (!"DRAFT".equals(e.getStatus()) && !"SCHEDULED".equals(e.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Exam can only be modified in DRAFT/SCHEDULED status");
    }
  }

  private void validateTimes(java.time.LocalTime start, java.time.LocalTime end, int duration) {
    if (start != null && end != null && !end.isAfter(start)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
    }
    if (duration <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationMinutes must be > 0");
    }
  }

  private void apply(Exam e, ExamRequest req) {
    e.setName(req.name());
    e.setExamType(req.examType());
    e.setClassName(req.className());
    e.setBoard(req.board());
    e.setExamDate(req.examDate());
    e.setStartTime(req.startTime());
    e.setEndTime(req.endTime());
    e.setDurationMinutes(req.durationMinutes());
    e.setFee(req.fee());
    e.setRegDeadline(req.regDeadline());
    if (req.maxAttempts() > 0) e.setMaxAttempts(req.maxAttempts());
    e.setInstructions(req.instructions());
    e.setMarksCorrect(req.marksCorrect());
    e.setNegativeMarks(req.negativeMarks());
    e.setMarksUnanswered(req.marksUnanswered());
    e.setResultDate(req.resultDate());
    e.setAnswerReview(req.answerReview());
    e.setCentres(req.centres());
  }

  private int nextOrder(Long examId) {
    return examQuestions.findByExamIdOrderByQuestionOrderAsc(examId).stream()
        .mapToInt(ExamQuestion::getQuestionOrder)
        .max()
        .orElse(0) + 1;
  }

  private String snapshot(Question q) {
    try {
      Map<String, Object> snap = new HashMap<>();
      snap.put("questionText", q.getQuestionText());
      snap.put("options", options.findByQuestionId(q.getId()).stream()
          .map(o -> {
            Map<String, Object> om = new HashMap<>();
            om.put("label", o.getLabel());
            om.put("optionText", o.getOptionText());
            om.put("correct", o.isCorrect());
            return om;
          })
          .toList());
      return objectMapper.writeValueAsString(snap);
    } catch (Exception e) {
      return null;
    }
  }
}
