package com.examora.auth.controller;

import com.examora.auth.dto.AuthResponse;
import com.examora.auth.dto.LoginRequest;
import com.examora.auth.dto.RegisterRequest;
import com.examora.auth.entity.Role;
import com.examora.auth.entity.User;
import com.examora.auth.repo.UserRepository;
import com.examora.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthController(
      UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
    String email = req.getEmail().trim().toLowerCase();
    if (users.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
    User user = new User();
    user.setFullName(req.getFullName());
    user.setEmail(email);
    user.setMobile(req.getMobile());
    user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
    user.setRole(req.getRole() != null ? req.getRole() : Role.STUDENT);
    user.setClassName(req.getClassName());
    user.setInstitute(req.getInstitute());
    user.setBoard(req.getBoard());
    user.setCity(req.getCity());
    user.setState(req.getState());
    user.setCountry(req.getCountry());
    user.setActive(true);
    users.save(user);

    String token = jwtService.generate(user.getEmail(), user.getRole().name());
    return new AuthResponse(token, user.getRole().name(), user.getId(), user.getEmail());
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest req) {
    String email = req.getEmail().trim().toLowerCase();
    User user = users.findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!user.isActive()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
    }
    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
    String token = jwtService.generate(user.getEmail(), user.getRole().name());
    return new AuthResponse(token, user.getRole().name(), user.getId(), user.getEmail());
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication authentication) {
    if (authentication == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
    }
    String email = (String) authentication.getPrincipal();
    User user = users.findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    Map<String, Object> out = new HashMap<>();
    out.put("userId", user.getId());
    out.put("email", user.getEmail());
    out.put("fullName", user.getFullName());
    out.put("role", user.getRole().name());
    out.put("mobile", user.getMobile());
    out.put("className", user.getClassName());
    out.put("institute", user.getInstitute());
    out.put("board", user.getBoard());
    out.put("city", user.getCity());
    out.put("state", user.getState());
    out.put("country", user.getCountry());
    return out;
  }

  @GetMapping("/validate")
  public Map<String, Object> validate(@RequestParam("token") String token) {
    try {
      Claims claims = jwtService.parse(token);
      String email = claims.getSubject();
      String role = claims.get("role", String.class);
      User user = users.findByEmail(email).orElse(null);
      Map<String, Object> out = new HashMap<>();
      out.put("valid", true);
      out.put("email", email);
      out.put("role", role);
      out.put("userId", user != null ? user.getId() : null);
      return out;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }
  }
}
