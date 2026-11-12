package com.examora.exam.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Markdown (Nougat) -> canonical LaTeX for question stems/options. No dependencies. */
public final class LatexConverter {

  private LatexConverter() {}

  // Verbatim math/env spans: never escaped or altered.
  private static final Pattern VERBATIM = Pattern.compile(
      "\\\\\\(.*?\\)|\\\\\\[.*?\\\\\\]|\\$\\$.*?\\$\\$|\\\\begin\\{[a-zA-Z*]+\\}.*?\\\\end\\{[a-zA-Z*]+\\}",
      Pattern.DOTALL);
  private static final Pattern HEADING = Pattern.compile("(?m)^#{1,6}\\s+(.+)$");
  private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
  private static final Pattern ITALIC = Pattern.compile("(^|\\W)(?:\\*(.+?)\\*|_(.+?)_)(?=\\W|$)");
  private static final Pattern CODE = Pattern.compile("`([^`\\n]+)`");
  private static final Pattern TABLE_SEP = Pattern.compile("(?m)^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?\\s*$");
  private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\(([^)]*)\\)");
  private static final Pattern MD_IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]*)\\)");
  private static final Pattern BLOCKQUOTE = Pattern.compile("(?m)^>\\s?(.*)$");
  private static final Pattern OL_ITEM = Pattern.compile("^\\s*\\d+\\.\\s+(.*)$");
  private static final Pattern UL_ITEM = Pattern.compile("^\\s*[-*]\\s+(.*)$");

  /** Convert markdown to LaTeX. Null/blank -> "". Verbatim math blocks pass through. */
  public static String toLatex(String markdown) {
    if (markdown == null || markdown.isBlank()) return "";
    // 1. Extract verbatim spans to placeholders.
    List<String> verbatim = new ArrayList<>();
    StringBuffer sb = new StringBuffer();
    Matcher vm = VERBATIM.matcher(markdown);
    while (vm.find()) {
      verbatim.add(vm.group());
      vm.appendReplacement(sb, "\u0000" + (verbatim.size() - 1) + "\u0000");
    }
    vm.appendTail(sb);
    String text = sb.toString();
    // 2. Drop markdown images (stored separately), keep links as text.
    text = MD_IMAGE.matcher(text).replaceAll("");
    text = LINK.matcher(text).replaceAll("$1");
    // 3. Headings -> \section*{}.
    text = HEADING.matcher(text).replaceAll(m -> "\\\\section*{" + m.group(1).trim() + "}");
    // 4. Tables -> tabular (line-based).
    text = convertTables(text);
    // 5. Lists / blockquotes (line-based).
    text = convertBlocks(text);
    // 6. Inline: code, bold, italic (in that order to avoid clashes).
    text = convertInline(text);
    // 7. Escape specials in plain text only (skip placeholders + emitted commands).
    text = escapePlain(text);
    // 8. Restore table separators, then verbatim spans.
    text = text.replace("\u0001", "&").replace("\u0002", "\\\\").replace("\u0003", "\\&");
    for (int i = 0; i < verbatim.size(); i++) {
      text = text.replace("\u0000" + i + "\u0000", verbatim.get(i));
    }
    return text.trim();
  }

  // ---- tables: | a | b | header + |---|---| sep -> \begin{tabular}...\hline...\end{tabular}
  private static String convertTables(String text) {
    String[] lines = text.split("\\R", -1);
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < lines.length) {
      if (isTableRow(lines[i]) && i + 1 < lines.length && TABLE_SEP.matcher(lines[i + 1]).matches()) {
        List<String> header = cells(lines[i]);
        int cols = header.size();
        List<List<String>> rows = new ArrayList<>();
        rows.add(header);
        i += 2;
        while (i < lines.length && isTableRow(lines[i])) {
          rows.add(cells(lines[i]));
          i++;
        }
        out.append("\\begin{tabular}{").append("|l".repeat(Math.max(cols, 1))).append("|}\n\\hline\n");
        for (List<String> r : rows) {
          // \u0001 = column sep, \u0002 = row break: shielded from escapePlain, restored at end.
          out.append(String.join(" \u0001 ", r)).append(" \u0002\n\\hline\n");
        }
        out.append("\\end{tabular}\n");
      } else {
        out.append(lines[i]).append("\n");
        i++;
      }
    }
    return out.toString();
  }

  private static boolean isTableRow(String line) {
    return line != null && line.contains("|") && line.trim().startsWith("|");
  }

  private static List<String> cells(String line) {
    String t = line.trim();
    if (t.startsWith("|")) t = t.substring(1);
    if (t.endsWith("|")) t = t.substring(0, t.length() - 1);
    List<String> out = new ArrayList<>();
    for (String c : t.split("\\|", -1)) out.add(c.trim());
    return out;
  }

  // ---- bullet/enumerate/quote groups
  private static String convertBlocks(String text) {
    String[] lines = text.split("\\R", -1);
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < lines.length) {
      if (UL_ITEM.matcher(lines[i]).matches()) {
        out.append("\\begin{itemize}\n");
        while (i < lines.length && UL_ITEM.matcher(lines[i]).matches()) {
          out.append("\\item ").append(UL_ITEM.matcher(lines[i]).replaceAll("$1")).append("\n");
          i++;
        }
        out.append("\\end{itemize}\n");
      } else if (OL_ITEM.matcher(lines[i]).matches()) {
        out.append("\\begin{enumerate}\n");
        while (i < lines.length && OL_ITEM.matcher(lines[i]).matches()) {
          out.append("\\item ").append(OL_ITEM.matcher(lines[i]).replaceAll("$1")).append("\n");
          i++;
        }
        out.append("\\end{enumerate}\n");
      } else if (lines[i].trim().startsWith(">")) {
        out.append("\\begin{quotation}\n");
        while (i < lines.length && lines[i].trim().startsWith(">")) {
          out.append(BLOCKQUOTE.matcher(lines[i]).replaceAll("$1")).append("\n");
          i++;
        }
        out.append("\\end{quotation}\n");
      } else {
        out.append(lines[i]).append("\n");
        i++;
      }
    }
    return out.toString();
  }

  private static String convertInline(String text) {
    text = CODE.matcher(text).replaceAll(m -> "\\\\texttt{" + m.group(1) + "}");
    text = BOLD.matcher(text).replaceAll(m -> "\\\\textbf{" + m.group(1) + "}");
    // Italic: preserve surrounding char in group 1.
    Matcher im = ITALIC.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (im.find()) {
      String inner = im.group(2) != null ? im.group(2) : im.group(3);
      im.appendReplacement(sb, Matcher.quoteReplacement(im.group(1) + "\\textit{" + inner + "}"));
    }
    im.appendTail(sb);
    return sb.toString();
  }

  // Escape & % $ # _ { } ~ ^ \\ outside placeholders and emitted commands.
  // Understands \\\\ line breaks and \\command[*]{arg} groups: braces structuring a
  // command argument are preserved, specials inside are still escaped.
  private static String escapePlain(String text) {
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '\u0000') { // verbatim placeholder -- copy through
        int j = text.indexOf('\u0000', i + 1);
        if (j < 0) j = i;
        out.append(text, i, j + 1);
        i = j + 1;
        continue;
      }
      if (c == '\\' && i + 1 < text.length()) {
        char n = text.charAt(i + 1);
        if (n == '\\') { out.append("\\\\"); i += 2; continue; } // tabular row break
        if (Character.isLetter(n)) { i = copyCommand(text, i, out); continue; }
      }
      if (c == '&') out.append("\u0003"); // literal ampersand; tabular seps use \u0001
      else out.append(escapeChar(c));
      i++;
    }
    return out.toString();
  }

  // Copy \\command[*] plus following {..}/[..] groups (escaping specials inside).
  private static int copyCommand(String text, int i, StringBuilder out) {
    int j = i + 1;
    while (j < text.length() && Character.isLetter(text.charAt(j))) j++;
    if (j < text.length() && text.charAt(j) == '*') j++;
    out.append(text, i, j);
    i = j;
    while (i < text.length() && (text.charAt(i) == '{' || text.charAt(i) == '[')) {
      char open = text.charAt(i);
      char close = open == '{' ? '}' : ']';
      out.append(open);
      i++;
      int depth = 1;
      while (i < text.length() && depth > 0) {
        char c = text.charAt(i);
        if (c == '\u0000') {
          int k = text.indexOf('\u0000', i + 1);
          if (k < 0) k = i;
          out.append(text, i, k + 1);
          i = k + 1;
          continue;
        }
        if (c == '\\' && i + 1 < text.length()) {
          char n = text.charAt(i + 1);
          if (n == '\\') { out.append("\\\\"); i += 2; continue; }
          if (Character.isLetter(n)) { i = copyCommand(text, i, out); continue; }
        }
        if (c == open) depth++;
        else if (c == close) { depth--; if (depth == 0) break; }
        if (depth > 0) out.append(c == open || c == close ? String.valueOf(c) : escapeChar(c));
        i++;
      }
      if (i < text.length()) { out.append(close); i++; }
    }
    return i;
  }

  private static String escapeChar(char c) {
    return switch (c) {
      case '&' -> "\\&";
      case '%' -> "\\%";
      case '$' -> "\\$";
      case '#' -> "\\#";
      case '_' -> "\\_";
      case '{' -> "\\{";
      case '}' -> "\\}";
      case '~' -> "\\textasciitilde{}";
      case '^' -> "\\textasciicircum{}";
      case '\\' -> "\\textbackslash{}";
      default -> String.valueOf(c);
    };
  }
}
