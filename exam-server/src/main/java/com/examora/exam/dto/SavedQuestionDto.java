package com.examora.exam.dto;

import java.util.List;

public record SavedQuestionDto(Long id, Long paperId, Integer questionNumber,
    Integer pageNumber, String questionText, String markdown,
    String questionType, Long subjectId, Long topicId, String difficulty,
    String examType, String imagePath, String verificationStatus,
    List<OptionDto> options, String correctAnswer,
    String latex, String imageBase64, String imageMime, String diagramBase64) {
  public record OptionDto(String label, String optionText, boolean correct, String optionLatex,
      String optionImageBase64) {}
}
