package kr.ucc.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

  @Test
  void quotesMultilineValuesAndProtectsSpreadsheetFormulas() {
    var encoded = Csv.encode(
      List.of(
        List.of("이름", "질문"),
        List.of("김,민서", "줄1\n\"줄2\""),
        List.of("  =1+1", "\t@SUM(1)")
      )
    );
    assertEquals(
      "\uFEFF\"이름\",\"질문\"\r\n\"김,민서\",\"줄1\n\"\"줄2\"\"\"\r\n\"'  =1+1\",\"'\t@SUM(1)\"\r\n",
      encoded
    );
  }
}
