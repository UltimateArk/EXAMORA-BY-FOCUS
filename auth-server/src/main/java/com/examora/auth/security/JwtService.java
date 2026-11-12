package com.examora.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey key;
  private final long expirationMs;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generate(String email, String role) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject(email)
        .claim("role", role)
        .issuedAt(new Date(now))
        .expiration(new Date(now + expirationMs))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      return false;
    }
  }

  public String extractEmail(String token) {
    return parse(token).getSubject();
  }

  public String extractRole(String token) {
    return parse(token).get("role", String.class);
  }
}
