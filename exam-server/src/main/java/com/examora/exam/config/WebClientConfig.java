package com.examora.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  @Bean
  public WebClient webClient(@Value("${extractor.url}") String extractorUrl,
      WebClient.Builder builder) {
    return builder.baseUrl(extractorUrl).build();
  }
}
