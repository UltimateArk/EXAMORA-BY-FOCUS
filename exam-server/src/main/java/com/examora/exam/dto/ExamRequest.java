package com.examora.exam.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// POJO (not a record): Jackson 3 message conversion proved unreliable for
// request records, so request DTOs stay classes. Response DTOs may be records.
public class ExamRequest {
  @NotBlank
  private String name;
  private String examType;
  private String className;
  private String board;
  private LocalDate examDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private int durationMinutes;
  private BigDecimal fee;
  private LocalDateTime regDeadline;
  private int maxAttempts;
  private String instructions;
  private Double marksCorrect;
  private Double negativeMarks;
  private double marksUnanswered;
  private LocalDate resultDate;
  private boolean answerReview;
  private String centres;

  public ExamRequest() {}

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
}
