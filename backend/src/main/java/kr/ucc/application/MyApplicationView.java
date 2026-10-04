package kr.ucc.application;

import java.time.Instant;

public record MyApplicationView(
  String key,
  String type,
  String title,
  String status,
  String path,
  Instant submittedAt
) {}
