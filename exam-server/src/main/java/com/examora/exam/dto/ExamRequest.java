package com.examora.exam.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ExamRequest(
    @NotBlank String name,
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
    Double marksCorrect,
    Double negativeMarks,
    double marksUnanswered,
    LocalDate resultDate,
    boolean answerReview,
    String centres) {}
