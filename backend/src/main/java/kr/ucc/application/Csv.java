package kr.ucc.application;

import java.util.List;
import java.util.stream.Collectors;

final class Csv {

  private Csv() {}

  static String encode(List<List<String>> rows) {
    return (
      "\uFEFF" +
      rows
        .stream()
        .map(row -> row.stream().map(Csv::cell).collect(Collectors.joining(",")))
        .collect(Collectors.joining("\r\n")) +
      "\r\n"
    );
  }

  private static String cell(String raw) {
    String value = raw == null ? "" : raw;
    String trimmed = value.stripLeading();
    // Spreadsheet programs must treat user-written titles and answers as text.
    if (
      (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) ||
      (!value.isEmpty() && "\t\r\n".indexOf(value.charAt(0)) >= 0)
    ) value = "'" + value;
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }
}
