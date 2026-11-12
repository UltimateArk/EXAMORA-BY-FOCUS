package com.examora.exam.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  // Boot 4 auto-configures Jackson 3 (tools.jackson); our code + jjwt use
  // Jackson 2 (com.fasterxml), so expose a Jackson 2 mapper explicitly.
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public WebClient webClient(@Value("${extractor.url}") String extractorUrl) {
    return WebClient.builder().baseUrl(extractorUrl).build();
  }
}
