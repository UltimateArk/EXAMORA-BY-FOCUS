package com.examora.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_papers")
public class QuestionPaper {
  public enum Status { UPLOADED, EXTRACTING, EXTRACTED, FAILED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "stored_path", nullable = false)
  private String storedPath;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.UPLOADED;

  @Column(name = "error_log", columnDefinition = "TEXT")
  private String errorLog;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @PrePersist
  void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public String getStoredPath() { return storedPath; }
  public void setStoredPath(String storedPath) { this.storedPath = storedPath; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public String getErrorLog() { return errorLog; }
  public void setErrorLog(String errorLog) { this.errorLog = errorLog; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
