package com.examora.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String fullName;

  @Column(nullable = false, unique = true)
  private String email;

  private String mobile;

  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.STUDENT;

  private String className;
  private String institute;
  private String board;
  private String city;
  private String state;
  private String country;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public User() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getMobile() { return mobile; }
  public void setMobile(String mobile) { this.mobile = mobile; }

  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }

  public String getClassName() { return className; }
  public void setClassName(String className) { this.className = className; }

  public String getInstitute() { return institute; }
  public void setInstitute(String institute) { this.institute = institute; }

  public String getBoard() { return board; }
  public void setBoard(String board) { this.board = board; }

  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }

  public String getState() { return state; }
  public void setState(String state) { this.state = state; }

  public String getCountry() { return country; }
  public void setCountry(String country) { this.country = country; }

  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
