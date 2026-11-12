package com.examora.exam.service;

import com.examora.exam.dto.PageText;
import com.examora.exam.entity.Question;
import com.examora.exam.entity.QuestionOption;
import com.examora.exam.entity.QuestionPaper;
import com.examora.exam.repo.QuestionOptionRepository;
import com.examora.exam.repo.QuestionPaperRepository;
import com.examora.exam.repo.QuestionRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionService {

  private static final Pattern Q_SPLIT =
      Pattern.compile("(?m)^\\s*(?:Q\\.?\\s*|Question\\s+)?(\\d{1,3})\\s*[\u002e):\\-]");
  private static final Pattern OPT =
      Pattern.compile("(?m)^\\s*\\(?([A-D])\\)?\\s*[\u002e):\\-]?\\s*(.+)$");
  private static final Pattern INT_ANS =
      Pattern.compile("(?i)answer\\s*[:\\-]\\s*(-?\\d+)");
  private static final Pattern NUM_ANS =
      Pattern.compile("(?i)(?:numerical|answer)\\s*[:\\-]\\s*(-?\\d+(?:\\.\\d+)?)");

  private final NougatClient nougat;
  private final PdfImageService pdfImages;
  private final StorageService storage;
  private final QuestionPaperRepository papers;
  private final QuestionRepository questions;
  private final QuestionOptionRepository options;

  public ExtractionService(NougatClient nougat, PdfImageService pdfImages,
      StorageService storage, QuestionPaperRepository papers,
      QuestionRepository questions, QuestionOptionRepository options) {
    this.nougat = nougat;
    this.pdfImages = pdfImages;
    this.storage = storage;
    this.papers = papers;
    this.questions = questions;
    this.options = options;
  }

  public List<PageText> extractText(Long paperId) {
    QuestionPaper paper = papers.findById(paperId).orElseThrow();
    paper.setStatus(QuestionPaper.Status.EXTRACTING);
    papers.save(paper);
    try {
      List<PageText> pages = nougat.extract(storage.paperPdf(paperId), paperId);
      paper.setStatus(QuestionPaper.Status.EXTRACTED);
      papers.save(paper);
      return pages;
    } catch (Exception e) {
      paper.setStatus(QuestionPaper.Status.FAILED);
      paper.setErrorLog(e.getMessage());
      papers.save(paper);
      throw e;
    }
  }

  public PdfImageService.RenderResult extractImages(Long paperId) throws Exception {
    return pdfImages.renderAndSplit(paperId);
  }

  @Transactional
  public int saveExtracted(Long paperId, String examType, Long subjectId,
      Long topicId, String difficulty) {
    QuestionPaper paper = papers.findById(paperId).orElseThrow();
    List<PageText> pages = nougat.extract(storage.paperPdf(paperId), paperId);

    Question.Difficulty diff;
    try {
      diff = difficulty == null ? Question.Difficulty.MEDIUM
          : Question.Difficulty.valueOf(difficulty.toUpperCase());
    } catch (IllegalArgumentException e) {
      diff = Question.Difficulty.MEDIUM;
    }

    int saved = 0;
    // Render + split crops once: crop files are sequential question-1..N across pages,
    // in the same order questions are saved below. Diagram detection: none yet.
    try {
      pdfImages.renderAndSplit(paperId);
    } catch (IOException e) {
      // proceed without crops
    }
    Path dir = null;
    try {
      dir = storage.papersDir(paperId);
    } catch (IOException e) {
      dir = null;
    }
    for (PageText page : pages) {
      List<ParsedQ> parsed = splitQuestions(page.extractedText());
      for (ParsedQ pq : parsed) {
        Question q = new Question();
        q.setPaper(paper);
        q.setQuestionNumber(pq.number() != null ? pq.number() : saved + 1);
        q.setPageNumber(page.pageNumber());
        q.setQuestionText(pq.stem());
        q.setMarkdown(pq.raw());
        q.setLatex(LatexConverter.toLatex(pq.stem()));
        q.setExamType(examType);
        q.setSubjectId(subjectId);
        q.setTopicId(topicId);
        q.setDifficulty(diff);
        q.setVerificationStatus(Question.VerificationStatus.PENDING);
        if (!pq.options().isEmpty()) {
          q.setQuestionType(Question.QuestionType.MCQ_SINGLE);
        } else if (pq.integerAnswer() != null) {
          q.setQuestionType(Question.QuestionType.INTEGER);
          q.setCorrectIntegerAnswer(pq.integerAnswer());
        } else if (pq.numericAnswer() != null) {
          q.setQuestionType(Question.QuestionType.NUMERIC);
          q.setCorrectNumericAnswer(pq.numericAnswer());
          q.setTolerance(0.01);
        } else {
          q.setQuestionType(Question.QuestionType.MCQ_SINGLE);
        }
        // Match crop file for global k (1-based in save order). Missing file -> leave null.
        int k = saved + 1;
        if (dir != null) {
          Path crop = dir.resolve("question-" + k + ".png");
          String b64 = storage.toBase64(crop);
          if (b64 != null) {
            q.setImagePath("papers/" + paperId + "/question-" + k + ".png");
            q.setImageBase64(b64);
          }
        }
        q.setDiagramBase64(null);
        questions.save(q);
        for (int i = 0; i < pq.options().size(); i++) {
          ParsedOpt o = pq.options().get(i);
          QuestionOption qo = new QuestionOption();
          qo.setQuestion(q);
          qo.setLabel(o.label());
          qo.setOptionText(o.text());
          qo.setOptionLatex(LatexConverter.toLatex(o.text()));
          qo.setCorrect(false);
          options.save(qo);
        }
        saved++;
      }
    }
    return saved;
  }

  record ParsedOpt(String label, String text) {}
  record ParsedQ(Integer number, String stem, String raw,
      List<ParsedOpt> options, Long integerAnswer, Double numericAnswer) {}

  List<ParsedQ> splitQuestions(String markdown) {
    List<ParsedQ> out = new ArrayList<>();
    if (markdown == null || markdown.isBlank()) return out;
    Matcher m = Q_SPLIT.matcher(markdown);
    List<int[]> bounds = new ArrayList<>();
    List<Integer> nums = new ArrayList<>();
    while (m.find()) {
      bounds.add(new int[]{m.start(), m.end()});
      try {
        nums.add(Integer.parseInt(m.group(1)));
      } catch (NumberFormatException e) {
        nums.add(null);
      }
    }
    if (bounds.isEmpty()) {
      out.add(parseBlock(null, markdown));
      return out;
    }
    for (int i = 0; i < bounds.size(); i++) {
      int start = bounds.get(i)[0];
      int end = (i + 1 < bounds.size()) ? bounds.get(i + 1)[0] : markdown.length();
      out.add(parseBlock(nums.get(i), markdown.substring(start, end).trim()));
    }
    return out;
  }

  ParsedQ parseBlock(Integer number, String raw) {
    String[] lines = raw.split("\\R");
    StringBuilder stem = new StringBuilder();
    List<ParsedOpt> opts = new ArrayList<>();
    for (String line : lines) {
      Matcher om = OPT.matcher(line);
      if (om.find() && stem.length() > 0) {
        opts.add(new ParsedOpt(om.group(1), om.group(2).trim()));
      } else {
        if (stem.length() > 0) stem.append("\n");
        stem.append(line);
      }
    }
    Long intAns = null;
    Double numAns = null;
    Matcher im = INT_ANS.matcher(raw);
    if (im.find()) {
      try {
        String s = im.group(1);
        if (s.contains(".")) numAns = Double.parseDouble(s);
        else intAns = Long.parseLong(s);
      } catch (NumberFormatException ignored) {}
    } else {
      Matcher nm = NUM_ANS.matcher(raw);
      if (nm.find()) {
        try {
          numAns = Double.parseDouble(nm.group(1));
        } catch (NumberFormatException ignored) {}
      }
    }
    return new ParsedQ(number, stem.toString().trim(), raw, opts, intAns, numAns);
  }
}
