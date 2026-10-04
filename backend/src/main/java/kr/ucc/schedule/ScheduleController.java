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

  private final kr.ucc.organization.MembershipRepository memberships;
  private final kr.ucc.personal.PersonalController personal;
  private final kr.ucc.program.ProgramRepository programs;
  private final kr.ucc.team.TeamRepository teams;
  private final kr.ucc.team.TeamApplicationRepository teamApplications;
  private final kr.ucc.team.TeamEntryRepository teamEntries;
  private final kr.ucc.organization.OrganizationWorkRepository work;
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
    MeetingService meetings,
    kr.ucc.organization.MembershipRepository memberships,
    kr.ucc.personal.PersonalController personal,
    kr.ucc.program.ProgramRepository programs,
    kr.ucc.team.TeamRepository teams,
    kr.ucc.team.TeamApplicationRepository teamApplications,
    kr.ucc.team.TeamEntryRepository teamEntries,
    kr.ucc.organization.OrganizationWorkRepository work
  ) {
    this.work = work;
    this.memberships = memberships;
    this.personal = personal;
    this.programs = programs;
    this.teams = teams;
    this.teamApplications = teamApplications;
    this.teamEntries = teamEntries;
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

  @GetMapping("/me/calendar")
  @Transactional(readOnly = true)
  List<View> personalCalendar(
    @RequestParam Instant from,
    @RequestParam Instant to,
    @RequestParam(required = false) Long org,
    Authentication a
  ) {
    if (
      !from.isBefore(to) || java.time.Duration.between(from, to).toDays() > 93
    ) throw ApiException.bad("INVALID_PERIOD", "조회 기간은 최대 93일까지입니다.");
    Long user = CurrentUser.id(a);
    var all = new LinkedHashMap<String, View>();
    var ids = new HashSet<Long>();
    for (var m : memberships.findByUserId(user)) ids.add(m.organizationId);
    if (org != null) ids.add(org);
    for (Long id : ids) for (var v : calendar(id, from, to, a)) all.put(v.key(), v);
    for (var p : programs.findAll())
      if (p.published) all.put(
        "program-" + p.id,
        new View(
          "program-" + p.id,
          p.id,
          "PROGRAM",
          p.title,
          p.startsAt,
          p.endsAt,
          p.category,
          "/programs/" + p.id,
          false
        )
      );
    for (var t : teams.findAll())
      if (
        t.ownerId.equals(user) ||
        teamApplications
          .findByTeamIdAndUserId(t.id, user)
          .filter(r -> r.status.equals("ACCEPTED"))
          .isPresent()
      ) for (var e : teamEntries.findByTeamIdOrderByIdDesc(t.id))
        if (Set.of("MEETING", "DEADLINE").contains(e.kind) && e.endsAt != null) {
          Instant start = e.startsAt != null ? e.startsAt : e.endsAt;
          all.put(
            "team-entry-" + e.id,
            new View(
              "team-entry-" + e.id,
              e.id,
              "TEAM",
              t.title + " · " + e.title,
              start,
              e.endsAt.equals(start) ? start.plusSeconds(1) : e.endsAt,
              e.content,
              "/teams/" + t.id,
              false
            )
          );
        }
    for (var m : memberships.findByUserId(user))
      for (var w : work.findByOrganizationIdOrderByIdDesc(m.organizationId))
        if (
          w.kind.equals("TASK") &&
          w.dueAt != null &&
          (user.equals(w.assigneeId) || m.role != Membership.Role.MEMBER)
        ) all.put(
          "work-" + w.id,
          new View(
            "work-" + w.id,
            w.id,
            "TASK",
            w.title + " · " + (w.status.equals("DONE") ? "완료" : "업무 마감"),
            w.dueAt,
            w.dueAt.plusSeconds(1),
            w.content,
            "/organization?org=" + m.organizationId,
            false
          )
        );
    for (var v : personal.list(a)) {
      String key = v.type().toLowerCase() + "-" + v.targetId();
      all.put(
        key,
        new View(
          key,
          v.targetId(),
          "SAVED",
          v.title(),
          v.startsAt(),
          v.endsAt(),
          "저장한 활동",
          v.path(),
          false
        )
      );
    }
    return all
      .values()
      .stream()
      .filter(v -> v.startsAt().isBefore(to) && v.endsAt().isAfter(from))
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
