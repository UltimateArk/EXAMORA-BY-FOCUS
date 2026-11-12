package com.examora.exam.service;

import com.examora.exam.dto.PageText;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NougatClient {

  private final WebClient webClient;
  private final ObjectMapper mapper = new ObjectMapper();

  public NougatClient(WebClient webClient) {
    this.webClient = webClient;
  }

  public List<PageText> extract(Path pdfPath) {
    try {
      MultipartBodyBuilder mb = new MultipartBodyBuilder();
      mb.part("file", new FileSystemResource(pdfPath.toFile()));
      String json = webClient.post().uri("/extract")
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(mb.build()))
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofMinutes(10))
          .block();
      return parse(json);
    } catch (Exception e) {
      throw new IllegalStateException("Extractor call failed: " + e.getMessage(), e);
    }
  }

  List<PageText> parse(String json) throws Exception {
    List<PageText> out = new ArrayList<>();
    if (json == null || json.isBlank()) return out;
    JsonNode root = mapper.readTree(json);
    JsonNode pages = root.has("pages") ? root.get("pages") : root;
    if (pages != null && pages.isArray()) {
      for (JsonNode p : pages) {
        int n = p.has("pageNumber") ? p.get("pageNumber").asInt()
            : p.has("page_number") ? p.get("page_number").asInt() : out.size() + 1;
        String text = p.has("extractedText") ? p.get("extractedText").asText()
            : p.has("markdown") ? p.get("markdown").asText()
            : p.has("text") ? p.get("text").asText() : "";
        out.add(new PageText(n, text));
      }
    }
    return out;
  }
}
