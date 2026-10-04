package kr.ucc.team;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.notification.Notifications;
import kr.ucc.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Transactional
public class TeamController {

  private final TeamRepository teams;
  private final TeamApplicationRepository applications;
  private final TeamEntryRepository entries;
  private final UserRepository users;
  private final Notifications notifications;

  public TeamController(
    TeamRepository teams,
    TeamApplicationRepository applications,
    TeamEntryRepository entries,
    UserRepository users,
    Notifications notifications
  ) {
    this.teams = teams;
    this.applications = applications;
    this.entries = entries;
    this.users = users;
    this.notifications = notifications;
  }

  public record Input(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 30000) String content,
    @NotBlank @Size(max = 1000) String roles,
    @Size(max = 500) String tags,
    @Min(2) @Max(100) int capacity,
    @NotNull Instant deadline
  ) {}

  public record StatusInput(@NotBlank String status) {}

  public record ApplyInput(
    @NotBlank @Size(max = 100) String role,
    @NotBlank @Size(max = 5000) String message
  ) {}

  public record Member(Long id, String name, String role) {}

  public record View(
    Team team,
    boolean owner,
    boolean member,
    int memberCount,
    TeamApplication mine
  ) {}

  public record Applicant(
    Long id,
    Long userId,
    String name,
    String role,
    String message,
    String status,
    Instant updatedAt
  ) {}

  public record EntryInput(
    @NotBlank String kind,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String content,
    Long assigneeId,
    Instant startsAt,
    Instant endsAt,
    @Size(max = 2000) String url,
    boolean done
  ) {}

  private Team lock(Long id) {
    return teams.lockById(id).orElseThrow(ApiException::missing);
  }

  private boolean member(Team t, Long user) {
    return (
      t.ownerId.equals(user) ||
      applications
        .findByTeamIdAndUserId(t.id, user)
        .filter(a -> a.status.equals("ACCEPTED"))
        .isPresent()
    );
  }

  private void owner(Team t, Long user) {
    if (!t.ownerId.equals(user)) throw ApiException.forbidden();
  }

  private void access(Team t, Long user) {
    if (!member(t, user)) throw ApiException.forbidden();
  }

  private int count(Team t) {
    return (
      1 +
      (int) applications
        .findByTeamId(t.id)
        .stream()
        .filter(a -> a.status.equals("ACCEPTED"))
        .count()
    );
  }

  private View view(Team t, Long user) {
    return new View(
      t,
      t.ownerId.equals(user),
      member(t, user),
      count(t),
      applications.findByTeamIdAndUserId(t.id, user).orElse(null)
    );
  }

  private void set(Team t, Input d) {
    if (t.id != null && d.capacity() < count(t)) throw ApiException.bad(
      "CAPACITY_TOO_SMALL",
      "구성원보다 정원을 작게 설정할 수 없습니다."
    );
    t.title = d.title().strip();
    t.content = d.content();
    t.roles = d.roles();
    t.tags = d.tags();
    t.capacity = d.capacity();
    t.deadline = d.deadline();
  }

  @GetMapping("/teams")
  List<View> list(
    @RequestParam(defaultValue = "") String search,
    @RequestParam(defaultValue = "false") boolean mine,
    Authentication a
  ) {
    Long u = CurrentUser.id(a);
    String q = search.toLowerCase(Locale.ROOT);
    return teams
      .findAll()
      .stream()
      .filter(t -> !mine || member(t, u))
      .filter(t ->
        (t.title + " " + t.content + " " + t.roles + " " + t.tags)
          .toLowerCase(Locale.ROOT)
          .contains(q)
      )
      .sorted(Comparator.comparing((Team t) -> t.id).reversed())
      .map(t -> view(t, u))
      .toList();
  }

  @PostMapping("/teams")
  View create(@Valid @RequestBody Input d, Authentication a) {
    var t = new Team();
    t.ownerId = CurrentUser.id(a);
    if (!d.deadline().isAfter(Instant.now())) throw ApiException.bad(
      "INVALID_PERIOD",
      "모집 마감은 현재 이후여야 합니다."
    );
    set(t, d);
    teams.save(t);
    return view(t, t.ownerId);
  }

  @GetMapping("/teams/{id}")
  View get(@PathVariable Long id, Authentication a) {
    return view(teams.findById(id).orElseThrow(ApiException::missing), CurrentUser.id(a));
  }

  @PatchMapping("/teams/{id}")
  View update(@PathVariable Long id, @Valid @RequestBody Input d, Authentication a) {
    var t = lock(id);
    Long u = CurrentUser.id(a);
    owner(t, u);
    if (t.status.equals("COMPLETED")) throw ApiException.bad(
      "TEAM_COMPLETED",
      "완료된 팀은 수정할 수 없습니다."
    );
    set(t, d);
    return view(t, u);
  }

  @PatchMapping("/teams/{id}/status")
  View status(@PathVariable Long id, @Valid @RequestBody StatusInput d, Authentication a) {
    var t = lock(id);
    Long u = CurrentUser.id(a);
    owner(t, u);
    if (
      !Set.of("OPEN", "CLOSED", "COMPLETED").contains(d.status()) || t.status.equals("COMPLETED")
    ) throw ApiException.bad("INVALID_STATUS", "팀 상태를 변경할 수 없습니다.");
    if (d.status().equals("OPEN") && !t.deadline.isAfter(Instant.now())) throw ApiException.bad(
      "INVALID_PERIOD",
      "모집 기간을 먼저 연장해 주세요."
    );
    t.status = d.status();
    return view(t, u);
  }

  @PostMapping("/teams/{id}/applications")
  View apply(@PathVariable Long id, @Valid @RequestBody ApplyInput d, Authentication a) {
    var t = lock(id);
    Long u = CurrentUser.id(a);
    if (member(t, u)) throw ApiException.bad("ALREADY_MEMBER", "이미 팀 구성원입니다.");
    if (
      !t.status.equals("OPEN") || !Instant.now().isBefore(t.deadline) || count(t) >= t.capacity
    ) throw ApiException.bad("CLOSED", "모집이 마감되었습니다.");
    var r = applications.findByTeamIdAndUserId(id, u).orElseGet(TeamApplication::new);
    if (r.id != null && r.status.equals("PENDING")) throw ApiException.bad(
      "DUPLICATE",
      "이미 검토 중인 지원입니다."
    );
    r.teamId = id;
    r.userId = u;
    r.role = d.role();
    r.message = d.message();
    r.status = "PENDING";
    r.updatedAt = Instant.now();
    applications.save(r);
    notifications.send(t.ownerId, "새 팀 지원: " + t.title, "/teams/" + id);
    return view(t, u);
  }

  @GetMapping("/teams/{id}/applications")
  List<Applicant> applicants(@PathVariable Long id, Authentication a) {
    var t = teams.findById(id).orElseThrow(ApiException::missing);
    owner(t, CurrentUser.id(a));
    return applications
      .findByTeamId(id)
      .stream()
      .map(r ->
        new Applicant(
          r.id,
          r.userId,
          users.findById(r.userId).orElseThrow().name,
          r.role,
          r.message,
          r.status,
          r.updatedAt
        )
      )
      .toList();
  }

  @PatchMapping("/teams/{id}/applications/{user}/status")
  View decide(
    @PathVariable Long id,
    @PathVariable Long user,
    @Valid @RequestBody StatusInput d,
    Authentication a
  ) {
    var t = lock(id);
    Long u = CurrentUser.id(a);
    owner(t, u);
    var r = applications.findByTeamIdAndUserId(id, user).orElseThrow(ApiException::missing);
    if (
      !r.status.equals("PENDING") ||
      !Set.of("ACCEPTED", "REJECTED").contains(d.status()) ||
      t.status.equals("COMPLETED")
    ) throw ApiException.bad("INVALID_STATUS", "검토 중인 지원만 처리할 수 있습니다.");
    if (d.status().equals("ACCEPTED") && count(t) >= t.capacity) throw ApiException.bad(
      "FULL",
      "팀 정원이 찼습니다."
    );
    r.status = d.status();
    r.updatedAt = Instant.now();
    notifications.send(user, "팀 지원 결과: " + t.title + " · " + d.status(), "/teams/" + id);
    return view(t, u);
  }

  @PostMapping("/teams/{id}/leave")
  View leave(@PathVariable Long id, Authentication a) {
    var t = lock(id);
    Long u = CurrentUser.id(a);
    if (t.ownerId.equals(u)) throw ApiException.bad(
      "OWNER_CANNOT_LEAVE",
      "팀장은 탈퇴할 수 없습니다."
    );
    if (t.status.equals("COMPLETED")) throw ApiException.bad(
      "TEAM_COMPLETED",
      "완료된 팀의 기록은 유지됩니다."
    );
    var r = applications.findByTeamIdAndUserId(id, u).orElseThrow(ApiException::missing);
    if (!Set.of("PENDING", "ACCEPTED").contains(r.status)) throw ApiException.bad(
      "INVALID_STATUS",
      "취소할 지원이 없습니다."
    );
    r.status = "CANCELLED";
    r.updatedAt = Instant.now();
    for (var e : entries.findByTeamIdOrderByIdDesc(id))
      if (u.equals(e.assigneeId)) e.assigneeId = null;
    return view(t, u);
  }

  @GetMapping("/teams/{id}/members")
  List<Member> members(@PathVariable Long id, Authentication a) {
    var t = teams.findById(id).orElseThrow(ApiException::missing);
    access(t, CurrentUser.id(a));
    var result = new ArrayList<Member>();
    result.add(new Member(t.ownerId, users.findById(t.ownerId).orElseThrow().name, "팀장"));
    for (var r : applications.findByTeamId(id))
      if (r.status.equals("ACCEPTED")) result.add(
        new Member(r.userId, users.findById(r.userId).orElseThrow().name, r.role)
      );
    return result;
  }

  @GetMapping("/teams/{id}/entries")
  List<TeamEntry> entries(@PathVariable Long id, Authentication a) {
    var t = teams.findById(id).orElseThrow(ApiException::missing);
    access(t, CurrentUser.id(a));
    return entries.findByTeamIdOrderByIdDesc(id);
  }

  private void setEntry(Team t, TeamEntry e, EntryInput d) {
    if (t.status.equals("COMPLETED")) throw ApiException.bad(
      "TEAM_COMPLETED",
      "완료된 팀의 기록은 수정할 수 없습니다."
    );
    if (
      !Set.of("MEETING", "DEADLINE", "TASK", "STAGE", "LINK").contains(d.kind())
    ) throw ApiException.bad("INVALID_KIND", "항목 유형을 확인해 주세요.");
    if (d.assigneeId() != null && !member(t, d.assigneeId())) throw ApiException.bad(
      "INVALID_ASSIGNEE",
      "담당자는 팀 구성원이어야 합니다."
    );
    if (
      d.kind().equals("MEETING") &&
      (d.startsAt() == null || d.endsAt() == null || !d.startsAt().isBefore(d.endsAt()))
    ) throw ApiException.bad("INVALID_PERIOD", "모임의 시작과 종료를 확인해 주세요.");
    if (d.kind().equals("DEADLINE") && d.endsAt() == null) throw ApiException.bad(
      "INVALID_PERIOD",
      "마감 시각이 필요합니다."
    );
    if (d.kind().equals("LINK") && (d.url() == null || d.url().isBlank())) throw ApiException.bad(
      "INVALID_URL",
      "자료 주소가 필요합니다."
    );
    if (d.url() != null && !d.url().isBlank()) try {
      var url = URI.create(d.url());
      if (
        !Set.of("http", "https").contains(url.getScheme()) ||
        url.getHost() == null ||
        url.getUserInfo() != null
      ) throw new IllegalArgumentException();
    } catch (Exception ex) {
      throw ApiException.bad("INVALID_URL", "http 또는 https 주소를 입력해 주세요.");
    }
    e.kind = d.kind();
    e.title = d.title();
    e.content = d.content();
    e.assigneeId = d.assigneeId();
    e.startsAt = d.startsAt();
    e.endsAt = d.endsAt();
    e.url = d.url();
    e.done = d.done();
  }

  @PostMapping("/teams/{id}/entries")
  TeamEntry entry(@PathVariable Long id, @Valid @RequestBody EntryInput d, Authentication a) {
    var t = lock(id);
    access(t, CurrentUser.id(a));
    var e = new TeamEntry();
    e.teamId = id;
    setEntry(t, e, d);
    return entries.save(e);
  }

  @PatchMapping("/teams/{id}/entries/{entry}")
  TeamEntry updateEntry(
    @PathVariable Long id,
    @PathVariable Long entry,
    @Valid @RequestBody EntryInput d,
    Authentication a
  ) {
    var t = lock(id);
    access(t, CurrentUser.id(a));
    var e = entries.findById(entry).orElseThrow(ApiException::missing);
    if (!e.teamId.equals(id)) throw ApiException.missing();
    setEntry(t, e, d);
    return e;
  }
}
