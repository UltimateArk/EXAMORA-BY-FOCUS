package com.examora.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "topics", indexes = @Index(columnList = "subject_id"))
public class Topic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "syllabus_type")
  private String syllabusType;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subject_id", nullable = false)
  private Subject subject;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getSyllabusType() { return syllabusType; }
  public void setSyllabusType(String syllabusType) { this.syllabusType = syllabusType; }
  public Subject getSubject() { return subject; }
  public void setSubject(Subject subject) { this.subject = subject; }
}
