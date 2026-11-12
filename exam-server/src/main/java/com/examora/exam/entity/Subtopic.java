package com.examora.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subtopics", indexes = @Index(columnList = "topic_id"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "name"}))
public class Subtopic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "topic_id", nullable = false)
  private Topic topic;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Topic getTopic() { return topic; }
  public void setTopic(Topic topic) { this.topic = topic; }
}
