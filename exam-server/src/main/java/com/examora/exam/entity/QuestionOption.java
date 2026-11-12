package com.examora.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "question_options", indexes = @Index(columnList = "question_id"))
public class QuestionOption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private Question question;

  @Column(nullable = false, length = 2)
  private String label;

  @Column(name = "option_text", columnDefinition = "TEXT")
  private String optionText;

  @Column(name = "option_image_path")
  private String optionImagePath;

  @Column(name = "option_latex", columnDefinition = "TEXT")
  private String optionLatex;

  @Column(nullable = false)
  private boolean correct = false;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Question getQuestion() { return question; }
  public void setQuestion(Question question) { this.question = question; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
  public String getOptionText() { return optionText; }
  public void setOptionText(String optionText) { this.optionText = optionText; }
  public String getOptionImagePath() { return optionImagePath; }
  public void setOptionImagePath(String p) { this.optionImagePath = p; }
  public String getOptionLatex() { return optionLatex; }
  public void setOptionLatex(String optionLatex) { this.optionLatex = optionLatex; }
  public boolean isCorrect() { return correct; }
  public void setCorrect(boolean correct) { this.correct = correct; }
}
