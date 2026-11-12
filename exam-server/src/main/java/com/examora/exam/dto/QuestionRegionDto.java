package com.examora.exam.dto;

public record QuestionRegionDto(int questionNumber, int pageNumber,
    int x, int y, int width, int height, String questionText) {}
