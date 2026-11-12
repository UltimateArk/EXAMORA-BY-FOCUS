package com.examora.exam.dto;

import java.util.List;

public record ExtractionResponse(Long paperId, String fileName,
    List<PageText> pages, List<PageImageDto> images,
    List<QuestionRegionDto> questions) {}
