package kr.ucc.archive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.event.*;
import kr.ucc.meeting.*;
import kr.ucc.organization.*;
import kr.ucc.recruitment.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Transactional
public class ArchiveController {

  private final HandoverNoteRepository notes;
  private final ExternalLinkRepository links;
  private final OrganizationAccess access;
  private final EventService events;
  private final RecruitmentService recruitments;
  private final MeetingService meetings;

  public ArchiveController(
    HandoverNoteRepository notes,
    ExternalLinkRepository links,
    OrganizationAccess access,
    EventService events,
    RecruitmentService recruitments,
    MeetingService meetings
  ) {
    this.notes = notes;
    this.links = links;
    this.access = access;
    this.events = events;
    this.recruitments = recruitments;
    this.meetings = meetings;
  }

  public record NoteInput(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 80) String period,
    @NotBlank @Size(max = 30000) String content
  ) {}

  public record LinkInput(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 2000) String url,
    @Size(max = 5000) String description
  ) {}

  public record Activity(
    String key,
    String type,
    Long id,
    String title,
    Instant date,
    String path,
    String status
  ) {}

  @GetMapping("/organizations/{org}/archive")
  @Transactional(readOnly = true)
  List<Activity> archive(
    @PathVariable Long org,
    @RequestParam(required = false) Integer year,
    Authentication a
  ) {
    long user = CurrentUser.id(a);
    access.member(org, user);
    var out = new ArrayList<Activity>();
    for (var e : events.list(org, user))
      if (e.status() == Event.Status.COMPLETED || e.status() == Event.Status.CANCELLED) out.add(
        new Activity(
          "event-" + e.id(),
          "EVENT",
          e.id(),
          e.title(),
          e.startsAt(),
          "/events/" + e.id(),
          e.status().name()
        )
      );
    for (var e : recruitments.list(org, user))
      if (
        e.status() == Recruitment.Status.CLOSED || e.status() == Recruitment.Status.CANCELLED
      ) out.add(
        new Activity(
          "recruitment-" + e.id(),
          "RECRUITMENT",
          e.id(),
          e.title(),
          e.closesAt(),
          "/recruitments/" + e.id(),
          e.status().name()
        )
      );
    for (var m : meetings.list(org, user))
      if (m.endsAt().isBefore(Instant.now())) out.add(
        new Activity(
          "meeting-" + m.id(),
          "MEETING",
          m.id(),
          m.title(),
          m.startsAt(),
          "/meetings/" + m.id(),
          "COMPLETED"
        )
      );
    return out
      .stream()
      .filter(
        e -> year == null || e.date().atZone(java.time.ZoneId.of("Asia/Seoul")).getYear() == year
      )
      .sorted(Comparator.comparing(Activity::date).reversed())
      .toList();
  }

  @GetMapping("/organizations/{org}/handover-notes")
  @Transactional(readOnly = true)
  List<HandoverNote.View> notes(@PathVariable Long org, Authentication a) {
    access.member(org, CurrentUser.id(a));
    return notes
      .findByOrganizationIdOrderByUpdatedAtDesc(org)
      .stream()
      .map(HandoverNote::view)
      .toList();
  }

  @PostMapping("/organizations/{org}/handover-notes")
  HandoverNote.View create(
    @PathVariable Long org,
    @Valid @RequestBody NoteInput d,
    Authentication a
  ) {
    access.staff(org, CurrentUser.id(a));
    var n = new HandoverNote();
    n.organizationId = org;
    set(n, d);
    return notes.save(n).view();
  }

  private void set(HandoverNote n, NoteInput d) {
    n.title = d.title();
    n.period = d.period();
    n.content = d.content();
    n.updatedAt = Instant.now();
  }

  @PatchMapping("/handover-notes/{id}")
  HandoverNote.View update(
    @PathVariable Long id,
    @Valid @RequestBody NoteInput d,
    Authentication a
  ) {
    var n = notes.findById(id).orElseThrow(ApiException::missing);
    access.staff(n.organizationId, CurrentUser.id(a));
    set(n, d);
    return n.view();
  }

  private void target(Long org, String type, Long id, Long user) {
    Long actual = switch (type) {
      case "EVENT" -> events.get(id, user).organizationId();
      case "RECRUITMENT" -> recruitments.get(id, user).organizationId();
      case "MEETING" -> meetings.get(id, user).organizationId();
      case "NOTE" -> notes.findById(id).orElseThrow(ApiException::missing).organizationId;
      default -> throw ApiException.bad("INVALID_TARGET", "지원하지 않는 자료 연결 대상입니다.");
    };
    if (!actual.equals(org)) throw ApiException.missing();
  }

  private void setLink(ExternalLink l, LinkInput d) {
    try {
      var uri = URI.create(d.url());
      if (
        !List.of("https", "http").contains(uri.getScheme()) ||
        uri.getHost() == null ||
        uri.getUserInfo() != null
      ) throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) {
      throw ApiException.bad("INVALID_URL", "올바른 http 또는 https 주소를 입력해 주세요.");
    }
    l.title = d.title();
    l.url = d.url();
    l.description = d.description();
  }

  @GetMapping("/organizations/{org}/links")
  @Transactional(readOnly = true)
  List<ExternalLink.View> links(
    @PathVariable Long org,
    @RequestParam String type,
    @RequestParam Long targetId,
    Authentication a
  ) {
    long user = CurrentUser.id(a);
    access.member(org, user);
    target(org, type, targetId, user);
    return links
      .findByOrganizationIdAndTargetTypeAndTargetId(org, type, targetId)
      .stream()
      .map(ExternalLink::view)
      .toList();
  }

  @PostMapping("/organizations/{org}/links")
  ExternalLink.View addLink(
    @PathVariable Long org,
    @RequestParam String type,
    @RequestParam Long targetId,
    @Valid @RequestBody LinkInput d,
    Authentication a
  ) {
    long user = CurrentUser.id(a);
    access.staff(org, user);
    target(org, type, targetId, user);
    var l = new ExternalLink();
    l.organizationId = org;
    l.targetType = type;
    l.targetId = targetId;
    setLink(l, d);
    return links.save(l).view();
  }

  @PatchMapping("/links/{id}")
  ExternalLink.View updateLink(
    @PathVariable Long id,
    @Valid @RequestBody LinkInput d,
    Authentication a
  ) {
    var l = links.findById(id).orElseThrow(ApiException::missing);
    access.staff(l.organizationId, CurrentUser.id(a));
    setLink(l, d);
    return l.view();
  }

  @DeleteMapping("/links/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void removeLink(@PathVariable Long id, Authentication a) {
    var l = links.findById(id).orElseThrow(ApiException::missing);
    access.staff(l.organizationId, CurrentUser.id(a));
    links.delete(l);
  }
}
