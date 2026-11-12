package com.examora.exam.service;

import com.examora.exam.dto.PageImageDto;
import com.examora.exam.dto.QuestionRegionDto;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class PdfImageService {

  private static final Pattern Q_HEADING =
      Pattern.compile("(?m)^\\s*(?:Q\\.?\\s*)?(\\d{1,3})\\s*[\\).\u02d0:-]");
  private static final int DPI = 150;

  private final StorageService storage;

  public PdfImageService(StorageService storage) {
    this.storage = storage;
  }

  public record RenderResult(List<PageImageDto> images, List<QuestionRegionDto> questions) {}

  public RenderResult renderAndSplit(Long paperId) throws IOException {
    Path pdf = storage.paperPdf(paperId);
    Path dir = storage.papersDir(paperId);
    List<PageImageDto> images = new ArrayList<>();
    List<QuestionRegionDto> regions = new ArrayList<>();

    try (PDDocument doc = PDDocument.load(pdf.toFile())) {
      PDFRenderer renderer = new PDFRenderer(doc);
      PDFTextStripper stripper = new PDFTextStripper();
      int pages = doc.getNumberOfPages();
      int globalQ = 1;
      for (int p = 0; p < pages; p++) {
        BufferedImage img = renderer.renderImageWithDPI(p, DPI);
        String pageFile = "page-" + (p + 1) + ".png";
        ImageIO.write(img, "PNG", dir.resolve(pageFile).toFile());
        images.add(new PageImageDto(p + 1, "papers/" + paperId + "/" + pageFile));

        stripper.setStartPage(p + 1);
        stripper.setEndPage(p + 1);
        String text;
        try {
          text = stripper.getText(doc);
        } catch (Exception e) {
          text = "";
        }
        Matcher m = Q_HEADING.matcher(text == null ? "" : text);
        int count = 0;
        while (m.find()) count++;
        if (count == 0) count = 1;

        int w = img.getWidth();
        int h = img.getHeight();
        int sliceH = Math.max(1, h / count);
        for (int i = 0; i < count; i++) {
          int y = i * sliceH;
          int sh = (i == count - 1) ? (h - y) : sliceH;
          BufferedImage slice = img.getSubimage(0, y, w, sh);
          String qFile = "question-" + globalQ + ".png";
          ImageIO.write(slice, "PNG", dir.resolve(qFile).toFile());
          String head = "Question " + globalQ + " (page " + (p + 1) + ")";
          regions.add(new QuestionRegionDto(globalQ, p + 1, 0, y, w, sh, head));
          globalQ++;
        }
      }
    }
    // ensure dir listing not required; return
    Files.createDirectories(dir);
    return new RenderResult(images, regions);
  }
}
