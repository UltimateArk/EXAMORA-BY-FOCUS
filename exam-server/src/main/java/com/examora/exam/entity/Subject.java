package com.examora.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Subject {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  public Subject() {}
  public Subject(String name) { this.name = name; }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
