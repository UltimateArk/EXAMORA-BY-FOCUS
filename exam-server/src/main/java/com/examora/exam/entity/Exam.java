package com.examora.exam.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "exams", indexes = {
    @Index(columnList = "status"),
    @Index(columnList = "exam_date")
})
public class Exam {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "exam_type")
  private String examType;

  @Column(name = "class_name")
  private String className;

  private String board;

  @Column(name = "exam_date")
  private LocalDate examDate;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(name = "duration_minutes")
  private int durationMinutes;

  private BigDecimal fee;

  @Column(name = "reg_deadline")
  private LocalDateTime regDeadline;

  @Column(name = "max_attempts", nullable = false)
  private int maxAttempts = 1;

  @Column(columnDefinition = "TEXT")
  private String instructions;

  @Column(nullable = false)
  private String status = "DRAFT";

  @Column(name = "marks_correct")
  private Double marksCorrect;

  @Column(name = "negative_marks")
  private Double negativeMarks;

  @Column(name = "marks_unanswered", nullable = false)
  private double marksUnanswered = 0;

  @Column(name = "result_date")
  private LocalDate resultDate;

  @Column(name = "answer_review", nullable = false)
  private boolean answerReview = false;

  @Column(columnDefinition = "TEXT")
  private String centres;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @PrePersist
  void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public String getClassName() { return className; }
  public void setClassName(String className) { this.className = className; }
  public String getBoard() { return board; }
  public void setBoard(String board) { this.board = board; }
  public LocalDate getExamDate() { return examDate; }
  public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
  public LocalTime getStartTime() { return startTime; }
  public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
  public LocalTime getEndTime() { return endTime; }
  public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
  public int getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
  public BigDecimal getFee() { return fee; }
  public void setFee(BigDecimal fee) { this.fee = fee; }
  public LocalDateTime getRegDeadline() { return regDeadline; }
  public void setRegDeadline(LocalDateTime regDeadline) { this.regDeadline = regDeadline; }
  public int getMaxAttempts() { return maxAttempts; }
  public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
  public String getInstructions() { return instructions; }
  public void setInstructions(String instructions) { this.instructions = instructions; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Double getMarksCorrect() { return marksCorrect; }
  public void setMarksCorrect(Double marksCorrect) { this.marksCorrect = marksCorrect; }
  public Double getNegativeMarks() { return negativeMarks; }
  public void setNegativeMarks(Double negativeMarks) { this.negativeMarks = negativeMarks; }
  public double getMarksUnanswered() { return marksUnanswered; }
  public void setMarksUnanswered(double marksUnanswered) { this.marksUnanswered = marksUnanswered; }
  public LocalDate getResultDate() { return resultDate; }
  public void setResultDate(LocalDate resultDate) { this.resultDate = resultDate; }
  public boolean isAnswerReview() { return answerReview; }
  public void setAnswerReview(boolean answerReview) { this.answerReview = answerReview; }
  public String getCentres() { return centres; }
  public void setCentres(String centres) { this.centres = centres; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
