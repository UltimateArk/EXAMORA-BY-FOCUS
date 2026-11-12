package com.examora.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
          UsernamePasswordAuthenticationToken auth =
              new UsernamePasswordAuthenticationToken(email, null, List.of(authority));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (Exception ex) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
