package com.examora.exam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final SecretKey key;

  public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        Claims claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
        String subject = claims.getSubject();
        Object roles = claims.get("roles");
        if (roles == null) {
          // auth-server issues the singular `role` claim (String).
          roles = claims.get("role");
        }
        List<String> names;
        if (roles instanceof List<?> list) {
          names = list.stream().map(Object::toString).toList();
        } else if (roles instanceof String s) {
          names = java.util.Arrays.stream(s.split(",")).map(String::trim)
              .filter(t -> !t.isEmpty()).toList();
        } else {
          names = List.of();
        }
        List<SimpleGrantedAuthority> authorities = names.isEmpty()
            ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
            : names.stream().map(n -> new SimpleGrantedAuthority("ROLE_" + n)).toList();
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(subject, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (Exception e) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
