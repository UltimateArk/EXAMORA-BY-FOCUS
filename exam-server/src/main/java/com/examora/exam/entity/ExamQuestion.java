package com.examora.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_questions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "question_id"}),
    indexes = @Index(columnList = "exam_id"))
public class ExamQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_id", nullable = false)
  private Exam exam;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private Question question;

  @Column(name = "question_order", nullable = false)
  private int questionOrder;

  @Column(name = "marks_override")
  private Double marksOverride;

  @Column(name = "snapshot_json", columnDefinition = "TEXT")
  private String snapshotJson;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Exam getExam() { return exam; }
  public void setExam(Exam exam) { this.exam = exam; }
  public Question getQuestion() { return question; }
  public void setQuestion(Question question) { this.question = question; }
  public int getQuestionOrder() { return questionOrder; }
  public void setQuestionOrder(int questionOrder) { this.questionOrder = questionOrder; }
  public Double getMarksOverride() { return marksOverride; }
  public void setMarksOverride(Double marksOverride) { this.marksOverride = marksOverride; }
  public String getSnapshotJson() { return snapshotJson; }
  public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }
}
