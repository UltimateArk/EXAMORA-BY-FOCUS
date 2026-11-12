package com.examora.exam.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

  private final Path root;

  public StorageService(@Value("${storage.dir:./storage}") String storageDir) {
    this.root = Paths.get(storageDir).toAbsolutePath().normalize();
  }

  public Path papersDir(Long paperId) throws IOException {
    Path dir = root.resolve("papers").resolve(String.valueOf(paperId));
    Files.createDirectories(dir);
    return dir;
  }

  public Path saveUpload(MultipartFile file, Long paperId) throws IOException {
    Path dir = papersDir(paperId);
    Path dest = dir.resolve(paperId + ".pdf");
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
    }
    return dest;
  }

  public Path paperPdf(Long paperId) {
    return root.resolve("papers").resolve(String.valueOf(paperId)).resolve(paperId + ".pdf");
  }

  public Path saveQuestionImage(Long paperId, String fileName, MultipartFile file)
      throws IOException {
    if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
      throw new IOException("Invalid file name");
    }
    Path dir = papersDir(paperId);
    Path dest = dir.resolve(fileName).normalize();
    if (!dest.startsWith(dir)) throw new IOException("Path traversal blocked");
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
    }
    return dest;
  }

  public Resource loadAsResource(Path path) throws IOException {
    Resource r = new UrlResource(path.toUri());
    if (!r.exists() || !r.isReadable()) throw new IOException("Not found: " + path);
    return r;
  }

  public Path resolveSecure(Long paperId, String fileName) throws IOException {
    Path dir = root.resolve("papers").resolve(String.valueOf(paperId)).normalize();
    Path target = dir.resolve(fileName).normalize();
    if (!target.startsWith(dir)) throw new IOException("Path traversal blocked");
    if (!Files.exists(target)) throw new IOException("Not found: " + fileName);
    return target;
  }

  /** Raw base64 (no data: prefix) of a file, or null if missing/unreadable. */
  public String toBase64(Path file) {
    try {
      if (file == null || !Files.exists(file)) return null;
      return Base64.getEncoder().encodeToString(Files.readAllBytes(file));
    } catch (IOException e) {
      return null;
    }
  }

  /** Sniff image mime from magic bytes: PNG -> image/png, JPEG SOI -> image/jpeg. */
  public static String sniffMime(byte[] b) {
    if (b != null && b.length >= 8
        && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) {
      return "image/png";
    }
    if (b != null && b.length >= 2
        && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
      return "image/jpeg";
    }
    return "application/octet-stream";
  }
}
