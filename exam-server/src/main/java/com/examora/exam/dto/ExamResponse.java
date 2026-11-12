package com.examora.exam.dto;

import com.examora.exam.entity.Exam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ExamResponse(
    Long id,
    String name,
    String examType,
    String className,
    String board,
    LocalDate examDate,
    LocalTime startTime,
    LocalTime endTime,
    int durationMinutes,
    BigDecimal fee,
    LocalDateTime regDeadline,
    int maxAttempts,
    String instructions,
    String status,
    Double marksCorrect,
    Double negativeMarks,
    double marksUnanswered,
    LocalDate resultDate,
    boolean answerReview,
    String centres,
    LocalDateTime createdAt) {

  public static ExamResponse from(Exam e) {
    return new ExamResponse(e.getId(), e.getName(), e.getExamType(), e.getClassName(),
        e.getBoard(), e.getExamDate(), e.getStartTime(), e.getEndTime(),
        e.getDurationMinutes(), e.getFee(), e.getRegDeadline(), e.getMaxAttempts(),
        e.getInstructions(), e.getStatus(), e.getMarksCorrect(), e.getNegativeMarks(),
        e.getMarksUnanswered(), e.getResultDate(), e.isAnswerReview(),
        e.getCentres(), e.getCreatedAt());
  }
}
