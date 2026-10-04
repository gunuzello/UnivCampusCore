package kr.ucc.application;

import java.nio.charset.StandardCharsets;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.event.EventService;
import kr.ucc.recruitment.RecruitmentService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ApplicantExportController {

  private final EventService events;
  private final RecruitmentService recruitments;

  public ApplicantExportController(EventService events, RecruitmentService recruitments) {
    this.events = events;
    this.recruitments = recruitments;
  }

  private record Row(
    Long id,
    String name,
    String email,
    String department,
    String studentNumber,
    String status,
    String submittedAt,
    List<String> answers
  ) {}

  @GetMapping("/events/{id}/applications/export")
  ResponseEntity<byte[]> events(
    @PathVariable Long id,
    @RequestParam(defaultValue = "") String search,
    @RequestParam(defaultValue = "") String status,
    Authentication authentication
  ) {
    long user = CurrentUser.id(authentication);
    var applicants = events.applicants(id, user);
    var activity = events.get(id, user);
    return export(
      "event-" + id,
      activity.questions(),
      applicants
        .stream()
        .map(a ->
          new Row(
            a.id(),
            a.name(),
            a.email(),
            a.department(),
            a.studentNumber(),
            a.status().name(),
            a.submittedAt().toString(),
            a.answers()
          )
        )
        .toList(),
      search,
      status,
      Set.of("REGISTERED", "ATTENDED", "ABSENT", "CANCELLED")
    );
  }

  @GetMapping("/recruitments/{id}/applications/export")
  ResponseEntity<byte[]> recruitments(
    @PathVariable Long id,
    @RequestParam(defaultValue = "") String search,
    @RequestParam(defaultValue = "") String status,
    Authentication authentication
  ) {
    long user = CurrentUser.id(authentication);
    var applicants = recruitments.applicants(id, user);
    var activity = recruitments.get(id, user);
    return export(
      "recruitment-" + id,
      activity.questions(),
      applicants
        .stream()
        .map(a ->
          new Row(
            a.id(),
            a.name(),
            a.email(),
            a.department(),
            a.studentNumber(),
            a.status().name(),
            a.submittedAt().toString(),
            a.answers()
          )
        )
        .toList(),
      search,
      status,
      Set.of("SUBMITTED", "REVIEWING", "ACCEPTED", "REJECTED", "CANCELLED")
    );
  }

  private ResponseEntity<byte[]> export(
    String name,
    List<String> questions,
    List<Row> rows,
    String search,
    String status,
    Set<String> statuses
  ) {
    if (
      search.length() > 200 || (!status.isEmpty() && !statuses.contains(status))
    ) throw ApiException.bad("INVALID_FILTER", "검색어 또는 상태 필터를 확인해 주세요.");
    String term = search.strip().toLowerCase(Locale.ROOT);
    var data = new ArrayList<List<String>>();
    var header = new ArrayList<>(
      List.of("신청번호", "이름", "이메일", "학과", "학번", "상태", "제출 시각(UTC)")
    );
    header.addAll(questions);
    data.add(header);
    for (var row : rows) {
      if (!status.isEmpty() && !status.equals(row.status())) continue;
      if (
        !term.isEmpty() &&
        !List.of(
          value(row.name()),
          value(row.email()),
          value(row.department()),
          value(row.studentNumber())
        )
          .stream()
          .anyMatch(v -> v.toLowerCase(Locale.ROOT).contains(term))
      ) continue;
      var cells = new ArrayList<>(
        List.of(
          row.id().toString(),
          value(row.name()),
          value(row.email()),
          value(row.department()),
          value(row.studentNumber()),
          label(row.status()),
          row.submittedAt()
        )
      );
      cells.addAll(row.answers());
      data.add(cells);
    }
    return ResponseEntity.ok()
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + name + "-applicants.csv\""
      )
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
      .body(Csv.encode(data).getBytes(StandardCharsets.UTF_8));
  }

  private String value(String value) {
    return value == null ? "" : value;
  }

  private String label(String status) {
    return switch (status) {
      case "REGISTERED" -> "신청 완료";
      case "ATTENDED" -> "참가 완료";
      case "ABSENT" -> "불참";
      case "SUBMITTED" -> "접수";
      case "REVIEWING" -> "검토 중";
      case "ACCEPTED" -> "합격";
      case "REJECTED" -> "불합격";
      case "CANCELLED" -> "취소";
      default -> status;
    };
  }
}
