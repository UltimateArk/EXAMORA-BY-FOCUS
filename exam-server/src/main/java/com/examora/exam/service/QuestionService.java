package com.examora.exam.service;

import com.examora.exam.dto.SavedQuestionDto;
import com.examora.exam.entity.Question;
import com.examora.exam.entity.QuestionOption;
import com.examora.exam.repo.QuestionOptionRepository;
import com.examora.exam.repo.QuestionRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionService {

  private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
  private static final Set<String> ALLOWED_IMAGE_TYPES =
      Set.of("image/png", "image/jpeg", "image/jpg");

  private final QuestionRepository questions;
  private final QuestionOptionRepository options;
  private final StorageService storage;

  public QuestionService(QuestionRepository questions, QuestionOptionRepository options,
      StorageService storage) {
    this.questions = questions;
    this.options = options;
    this.storage = storage;
  }

  public List<SavedQuestionDto> listByPaper(Long paperId) {
    return questions.findByPaperId(paperId).stream().map(this::toDto).toList();
  }

  public List<SavedQuestionDto> generate(Long subjectId, Long topicId,
      String difficulty, String examType, int count) {
    List<Question> found = questions.findRandomByFilters(subjectId, topicId,
        difficulty == null ? null : difficulty.toUpperCase(), examType, count);
    return found.stream().map(this::toDto).toList();
  }

  @Transactional
  public SavedQuestionDto verify(Long id, String status) {
    Question q = questions.findById(id).orElseThrow();
    q.setVerificationStatus(Question.VerificationStatus.valueOf(status.toUpperCase()));
    return toDto(questions.save(q));
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public SavedQuestionDto update(Long id, Map<String, Object> body) {
    Question q = questions.findById(id).orElseThrow();
    if (body.containsKey("questionText")) q.setQuestionText((String) body.get("questionText"));
    if (body.containsKey("markdown")) q.setMarkdown((String) body.get("markdown"));
    if (body.containsKey("latex")) q.setLatex((String) body.get("latex"));
    if (body.containsKey("explanation")) q.setExplanation((String) body.get("explanation"));
    if (body.containsKey("tolerance") && body.get("tolerance") instanceof Number n) {
      q.setTolerance(n.doubleValue());
    }
    if (body.containsKey("difficulty")) {
      try {
        q.setDifficulty(Question.Difficulty.valueOf(((String) body.get("difficulty")).toUpperCase()));
      } catch (Exception ignored) {}
    }
    if (body.containsKey("questionType")) {
      try {
        q.setQuestionType(Question.QuestionType.valueOf(((String) body.get("questionType")).toUpperCase()));
      } catch (Exception ignored) {}
    }
    if (body.containsKey("correctIntegerAnswer") && body.get("correctIntegerAnswer") != null) {
      q.setCorrectIntegerAnswer(Long.valueOf(body.get("correctIntegerAnswer").toString()));
    }
    if (body.containsKey("correctNumericAnswer") && body.get("correctNumericAnswer") != null) {
      q.setCorrectNumericAnswer(Double.valueOf(body.get("correctNumericAnswer").toString()));
    }
    if (body.containsKey("correctAnswer") && body.get("correctAnswer") instanceof String s) {
      try {
        if (q.getQuestionType() == Question.QuestionType.INTEGER) {
          q.setCorrectIntegerAnswer(Long.valueOf(s.trim()));
          q.setCorrectNumericAnswer(null);
        } else if (q.getQuestionType() == Question.QuestionType.NUMERIC) {
          q.setCorrectNumericAnswer(Double.valueOf(s.trim()));
          q.setCorrectIntegerAnswer(null);
        }
      } catch (NumberFormatException ignored) {}
    }
    if (body.containsKey("removeImage")
        && Boolean.parseBoolean(String.valueOf(body.get("removeImage")))) {
      q.setImagePath(null);
      q.setImageBase64(null);
    } else if (body.containsKey("imageUrl") && body.get("imageUrl") == null) {
      q.setImagePath(null);
      q.setImageBase64(null);
    }
    if (body.containsKey("options") && body.get("options") instanceof List<?> list) {
      options.deleteByQuestionId(id);
      for (Object o : list) {
        if (o instanceof Map<?, ?> om) {
          QuestionOption qo = new QuestionOption();
          qo.setQuestion(q);
          qo.setLabel(String.valueOf(om.getOrDefault("label", "A")));
          qo.setOptionText(String.valueOf(om.getOrDefault("optionText", "")));
          if (om.get("optionLatex") != null) qo.setOptionLatex(String.valueOf(om.get("optionLatex")));
          Object c = om.get("correct");
          qo.setCorrect(c instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(c)));
          options.save(qo);
        }
      }
    }
    return toDto(questions.save(q));
  }

  @Transactional
  public SavedQuestionDto replaceImage(Long id, MultipartFile file) {
    Question q = questions.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + id));
    validateImage(file);
    try {
      int n = q.getQuestionNumber() != null ? q.getQuestionNumber() : q.getId().intValue();
      Long paperId = q.getPaper() != null ? q.getPaper().getId() : null;
      if (paperId == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Question has no paper; cannot store image");
      }
      String fileName = "question-" + n + "-custom.png";
      storage.saveQuestionImage(paperId, fileName, file);
      q.setImagePath("papers/" + paperId + "/" + fileName);
      try {
        q.setImageBase64(java.util.Base64.getEncoder().encodeToString(file.getBytes()));
      } catch (Exception e) {
        q.setImageBase64(null);
      }
      return toDto(questions.save(q));
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to store image", e);
    }
  }

  @Transactional
  public SavedQuestionDto clearImage(Long id) {
    Question q = questions.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + id));
    q.setImagePath(null);
    q.setImageBase64(null);
    return toDto(questions.save(q));
  }

  private void validateImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
    }
    if (file.getSize() > MAX_IMAGE_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Image must be at most 10MB");
    }
    String contentType = file.getContentType();
    String name = file.getOriginalFilename() == null
        ? "" : file.getOriginalFilename().toLowerCase();
    boolean typeOk = contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    boolean extOk = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    if (!typeOk && !extOk) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Only png/jpg images are allowed");
    }
  }

  public SavedQuestionDto toDto(Question q) {
    Long qid = q.getId();
    List<SavedQuestionDto.OptionDto> opts = options.findByQuestionId(qid).stream()
        .map(o -> new SavedQuestionDto.OptionDto(o.getLabel(), o.getOptionText(), o.isCorrect(),
            o.getOptionLatex()))
        .toList();
    String correct = null;
    if (q.getCorrectIntegerAnswer() != null) correct = String.valueOf(q.getCorrectIntegerAnswer());
    else if (q.getCorrectNumericAnswer() != null) correct = String.valueOf(q.getCorrectNumericAnswer());
    else {
      StringBuilder sb = new StringBuilder();
      for (var o : opts) if (o.correct()) sb.append(o.label());
      if (sb.length() > 0) correct = sb.toString();
    }
    Long paperId = null;
    try {
      paperId = q.getPaper() != null ? q.getPaper().getId() : null;
    } catch (Exception ignored) {}
    String imageMime = "image/png";
    if (q.getImagePath() != null && paperId != null) {
      try {
        String fileName = q.getImagePath().substring(q.getImagePath().lastIndexOf('/') + 1);
        byte[] bytes = java.nio.file.Files.readAllBytes(storage.resolveSecure(paperId, fileName));
        imageMime = StorageService.sniffMime(bytes);
      } catch (Exception ignored) {}
    }
    return new SavedQuestionDto(qid, paperId, q.getQuestionNumber(), q.getPageNumber(),
        q.getQuestionText(), q.getMarkdown(),
        q.getQuestionType() == null ? null : q.getQuestionType().name(),
        q.getSubjectId(), q.getTopicId(),
        q.getDifficulty() == null ? null : q.getDifficulty().name(),
        q.getExamType(), q.getImagePath(),
        q.getVerificationStatus() == null ? null : q.getVerificationStatus().name(),
        opts, correct, q.getLatex(), q.getImageBase64(), imageMime, q.getDiagramBase64());
  }
}
