package com.examora.auth.dto;

import com.examora.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

  @NotBlank
  private String fullName;

  @NotBlank
  @Email
  private String email;

  private String mobile;

  @NotBlank
  @Size(min = 6, max = 100)
  private String password;

  private Role role;

  private String className;
  private String institute;
  private String board;
  private String city;
  private String state;
  private String country;

  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getMobile() { return mobile; }
  public void setMobile(String mobile) { this.mobile = mobile; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

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
}
