package com.examora.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "questions", indexes = {
    @Index(columnList = "paper_id"),
    @Index(columnList = "subject_id"),
    @Index(columnList = "topic_id"),
    @Index(columnList = "verification_status")
})
public class Question {
  public enum QuestionType { MCQ_SINGLE, MCQ_MULTI, INTEGER, NUMERIC }
  public enum Difficulty { EASY, MEDIUM, HARD }
  public enum VerificationStatus { PENDING, APPROVED, REJECTED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paper_id", nullable = false)
  private QuestionPaper paper;

  @Column(name = "question_number")
  private Integer questionNumber;

  @Column(name = "page_number")
  private Integer pageNumber;

  @Column(name = "question_text", columnDefinition = "TEXT")
  private String questionText;

  @Column(columnDefinition = "TEXT")
  private String markdown;

  @Column(name = "latex_json", columnDefinition = "TEXT")
  private String latexJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "question_type")
  private QuestionType questionType = QuestionType.MCQ_SINGLE;

  @Column(name = "subject_id")
  private Long subjectId;

  @Column(name = "topic_id")
  private Long topicId;

  @Column(name = "subtopic_id")
  private Long subtopicId;

  @Enumerated(EnumType.STRING)
  private Difficulty difficulty = Difficulty.MEDIUM;

  @Column(name = "exam_type")
  private String examType;

  @Column(name = "image_path")
  private String imagePath;

  @Column(name = "diagram_path")
  private String diagramPath;

  @Column(name = "correct_integer_answer")
  private Long correctIntegerAnswer;

  @Column(name = "correct_numeric_answer")
  private Double correctNumericAnswer;

  private Double tolerance = 0.0;

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status", nullable = false)
  private VerificationStatus verificationStatus = VerificationStatus.PENDING;

  @Column(columnDefinition = "TEXT")
  private String explanation;

  @Column(columnDefinition = "TEXT")
  private String latex;

  @Column(name = "image_base64", columnDefinition = "TEXT")
  private String imageBase64;

  @Column(name = "diagram_base64", columnDefinition = "TEXT")
  private String diagramBase64;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public QuestionPaper getPaper() { return paper; }
  public void setPaper(QuestionPaper paper) { this.paper = paper; }
  public Integer getQuestionNumber() { return questionNumber; }
  public void setQuestionNumber(Integer questionNumber) { this.questionNumber = questionNumber; }
  public Integer getPageNumber() { return pageNumber; }
  public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
  public String getQuestionText() { return questionText; }
  public void setQuestionText(String questionText) { this.questionText = questionText; }
  public String getMarkdown() { return markdown; }
  public void setMarkdown(String markdown) { this.markdown = markdown; }
  public String getLatexJson() { return latexJson; }
  public void setLatexJson(String latexJson) { this.latexJson = latexJson; }
  public QuestionType getQuestionType() { return questionType; }
  public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
  public Long getSubjectId() { return subjectId; }
  public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
  public Long getTopicId() { return topicId; }
  public void setTopicId(Long topicId) { this.topicId = topicId; }
  public Long getSubtopicId() { return subtopicId; }
  public void setSubtopicId(Long subtopicId) { this.subtopicId = subtopicId; }
  public Difficulty getDifficulty() { return difficulty; }
  public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public String getImagePath() { return imagePath; }
  public void setImagePath(String imagePath) { this.imagePath = imagePath; }
  public String getDiagramPath() { return diagramPath; }
  public void setDiagramPath(String diagramPath) { this.diagramPath = diagramPath; }
  public Long getCorrectIntegerAnswer() { return correctIntegerAnswer; }
  public void setCorrectIntegerAnswer(Long v) { this.correctIntegerAnswer = v; }
  public Double getCorrectNumericAnswer() { return correctNumericAnswer; }
  public void setCorrectNumericAnswer(Double v) { this.correctNumericAnswer = v; }
  public Double getTolerance() { return tolerance; }
  public void setTolerance(Double tolerance) { this.tolerance = tolerance; }
  public VerificationStatus getVerificationStatus() { return verificationStatus; }
  public void setVerificationStatus(VerificationStatus s) { this.verificationStatus = s; }
  public String getExplanation() { return explanation; }
  public void setExplanation(String explanation) { this.explanation = explanation; }
  public String getLatex() { return latex; }
  public void setLatex(String latex) { this.latex = latex; }
  public String getImageBase64() { return imageBase64; }
  public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
  public String getDiagramBase64() { return diagramBase64; }
  public void setDiagramBase64(String diagramBase64) { this.diagramBase64 = diagramBase64; }
}
