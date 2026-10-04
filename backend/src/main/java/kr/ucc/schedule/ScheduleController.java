package kr.ucc.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class ScheduleController {

  private final ScheduleRepository schedules;
  private final OrganizationAccess access;
  private final EventService events;
  private final RecruitmentService recruitments;
  private final MeetingService meetings;

  public ScheduleController(
    ScheduleRepository schedules,
    OrganizationAccess access,
    EventService events,
    RecruitmentService recruitments,
    MeetingService meetings
  ) {
    this.schedules = schedules;
    this.access = access;
    this.events = events;
    this.recruitments = recruitments;
    this.meetings = meetings;
  }

  public record Input(
    @NotBlank @Size(max = 200) String title,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Size(max = 10000) String description
  ) {}

  public record View(
    String key,
    Long id,
    String type,
    String title,
    Instant startsAt,
    Instant endsAt,
    String description,
    String path,
    boolean canManage
  ) {}

  private View view(Schedule s, boolean manage) {
    return new View(
      "schedule-" + s.id,
      s.id,
      "SCHEDULE",
      s.title,
      s.startsAt,
      s.endsAt,
      s.description,
      "/schedules/" + s.id,
      manage
    );
  }

  private void set(Schedule s, Input d) {
    if (!d.startsAt().isBefore(d.endsAt())) throw ApiException.bad(
      "INVALID_PERIOD",
      "일정 종료는 시작 이후여야 합니다."
    );
    s.title = d.title();
    s.startsAt = d.startsAt();
    s.endsAt = d.endsAt();
    s.description = d.description();
  }

  @GetMapping("/organizations/{org}/calendar")
  @Transactional(readOnly = true)
  List<View> calendar(
    @PathVariable Long org,
    @RequestParam Instant from,
    @RequestParam Instant to,
    Authentication a
  ) {
    if (
      !from.isBefore(to) || java.time.Duration.between(from, to).toDays() > 93
    ) throw ApiException.bad("INVALID_PERIOD", "조회 기간은 1일부터 최대 93일까지입니다.");
    long user = CurrentUser.id(a);
    var out = new ArrayList<View>();
    boolean member = false,
      manage = false;
    try {
      var m = access.member(org, user);
      member = true;
      manage = m.role != Membership.Role.MEMBER;
    } catch (ApiException ignored) {}
    for (var e : events.list(org, user))
      if (e.status() != Event.Status.CANCELLED) out.add(
        new View(
          "event-" + e.id(),
          e.id(),
          "EVENT",
          e.title(),
          e.startsAt(),
          e.endsAt(),
          e.description(),
          "/events/" + e.id(),
          false
        )
      );
    for (var e : recruitments.list(org, user))
      if (e.status() != Recruitment.Status.CANCELLED) out.add(
        new View(
          "recruitment-" + e.id(),
          e.id(),
          "RECRUITMENT",
          e.title(),
          e.opensAt(),
          e.closesAt(),
          e.description(),
          "/recruitments/" + e.id(),
          false
        )
      );
    if (member) {
      for (var m : meetings.list(org, user))
        out.add(
          new View(
            "meeting-" + m.id(),
            m.id(),
            "MEETING",
            m.title(),
            m.startsAt(),
            m.endsAt(),
            m.content(),
            "/meetings/" + m.id(),
            false
          )
        );
      for (var s : schedules.findByOrganizationIdOrderByStartsAtAsc(org)) out.add(view(s, manage));
    }
    return out
      .stream()
      .filter(s -> s.startsAt().isBefore(to) && s.endsAt().isAfter(from))
      .sorted(Comparator.comparing(View::startsAt))
      .toList();
  }

  @PostMapping("/organizations/{org}/schedules")
  View create(@PathVariable Long org, @Valid @RequestBody Input d, Authentication a) {
    access.staff(org, CurrentUser.id(a));
    var s = new Schedule();
    s.organizationId = org;
    set(s, d);
    return view(schedules.save(s), true);
  }

  public record Detail(View schedule, Long organizationId) {}

  @GetMapping("/schedules/{id}")
  @Transactional(readOnly = true)
  Detail get(@PathVariable Long id, Authentication a) {
    var s = schedules.findById(id).orElseThrow(ApiException::missing);
    var member = access.member(s.organizationId, CurrentUser.id(a));
    return new Detail(view(s, member.role != Membership.Role.MEMBER), s.organizationId);
  }

  @PatchMapping("/schedules/{id}")
  View update(@PathVariable Long id, @Valid @RequestBody Input d, Authentication a) {
    var s = schedules.findById(id).orElseThrow(ApiException::missing);
    access.staff(s.organizationId, CurrentUser.id(a));
    set(s, d);
    return view(s, true);
  }

  @DeleteMapping("/schedules/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long id, Authentication a) {
    var s = schedules.findById(id).orElseThrow(ApiException::missing);
    access.staff(s.organizationId, CurrentUser.id(a));
    schedules.delete(s);
  }
}
