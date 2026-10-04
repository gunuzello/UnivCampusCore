package kr.ucc.personal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.event.EventService;
import kr.ucc.organization.OrganizationAccess;
import kr.ucc.program.*;
import kr.ucc.recruitment.RecruitmentService;
import kr.ucc.team.*;
import kr.ucc.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@Transactional
public class PersonalController {

  private final InterestProfileRepository profiles;
  private final SavedActivityRepository saved;
  private final UserRepository users;
  private final ProgramRepository programs;
  private final TeamRepository teams;
  private final EventService events;
  private final RecruitmentService recruitments;
  private final OrganizationAccess access;

  public PersonalController(
    InterestProfileRepository profiles,
    SavedActivityRepository saved,
    UserRepository users,
    ProgramRepository programs,
    TeamRepository teams,
    EventService events,
    RecruitmentService recruitments,
    OrganizationAccess access
  ) {
    this.profiles = profiles;
    this.saved = saved;
    this.users = users;
    this.programs = programs;
    this.teams = teams;
    this.events = events;
    this.recruitments = recruitments;
    this.access = access;
  }

  public record ProfileInput(
    @Size(max = 2000) String interests,
    @Size(max = 2000) String activities,
    @Size(max = 4000) String courses,
    @Size(max = 4000) String skills,
    @Size(max = 10000) String portfolio
  ) {}

  public record SavedView(
    String type,
    Long targetId,
    String title,
    String path,
    Instant startsAt,
    Instant endsAt
  ) {}

  public record Recommendation(
    String type,
    Long id,
    String title,
    String path,
    String reason,
    int score
  ) {}

  @GetMapping("/interests")
  InterestProfile profile(Authentication a) {
    return profiles.findById(CurrentUser.id(a)).orElseGet(InterestProfile::new);
  }

  @PutMapping("/interests")
  InterestProfile set(@Valid @RequestBody ProfileInput d, Authentication a) {
    Long u = CurrentUser.id(a);
    users.lockById(u).orElseThrow(ApiException::missing);
    var p = profiles.findById(u).orElseGet(InterestProfile::new);
    p.userId = u;
    p.interests = d.interests();
    p.activities = d.activities();
    p.courses = d.courses();
    p.skills = d.skills();
    p.portfolio = d.portfolio();
    return profiles.save(p);
  }

  private SavedView resolve(String type, Long id, Long user) {
    switch (type) {
      case "PROGRAM": {
        var p = programs.findById(id).orElseThrow(ApiException::missing);
        if (!p.published) access.staff(p.organizationId, user);
        return new SavedView(type, id, p.title, "/programs/" + id, p.startsAt, p.endsAt);
      }
      case "TEAM": {
        var t = teams.findById(id).orElseThrow(ApiException::missing);
        return new SavedView(
          type,
          id,
          t.title,
          "/teams/" + id,
          t.deadline,
          t.deadline.plusSeconds(1)
        );
      }
      case "EVENT": {
        var e = events.get(id, user);
        return new SavedView(type, id, e.title(), "/events/" + id, e.startsAt(), e.endsAt());
      }
      case "RECRUITMENT": {
        var r = recruitments.get(id, user);
        return new SavedView(type, id, r.title(), "/recruitments/" + id, r.opensAt(), r.closesAt());
      }
      default:
        throw ApiException.bad("INVALID_TYPE", "저장할 활동 유형을 확인해 주세요.");
    }
  }

  @GetMapping("/saved")
  public List<SavedView> list(Authentication a) {
    Long user = CurrentUser.id(a);
    return saved
      .findByUserIdOrderByIdDesc(user)
      .stream()
      .map(s -> {
        try {
          return resolve(s.type, s.targetId, user);
        } catch (ApiException e) {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();
  }

  @PutMapping("/saved/{type}/{id}")
  SavedView save(@PathVariable String type, @PathVariable Long id, Authentication a) {
    Long u = CurrentUser.id(a);
    users.lockById(u).orElseThrow(ApiException::missing);
    var view = resolve(type, id, u);
    if (saved.findByUserIdAndTypeAndTargetId(u, type, id).isEmpty()) {
      var s = new SavedActivity();
      s.userId = u;
      s.type = type;
      s.targetId = id;
      saved.save(s);
    }
    return view;
  }

  @DeleteMapping("/saved/{type}/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void unsave(@PathVariable String type, @PathVariable Long id, Authentication a) {
    Long u = CurrentUser.id(a);
    users.lockById(u).orElseThrow(ApiException::missing);
    saved.findByUserIdAndTypeAndTargetId(u, type, id).ifPresent(saved::delete);
  }

  @GetMapping("/recommendations")
  List<Recommendation> recommend(Authentication a) {
    var p = profiles.findById(CurrentUser.id(a)).orElseGet(InterestProfile::new);
    var terms = Arrays.stream(
      (Objects.toString(p.interests, "") + "," + Objects.toString(p.activities, "")).split("[,\n]")
    )
      .map(String::strip)
      .filter(s -> !s.isBlank())
      .distinct()
      .limit(30)
      .toList();
    var result = new ArrayList<Recommendation>();
    for (var program : programs.findAll())
      if (program.published && Instant.now().isBefore(program.deadline)) add(
        result,
        "PROGRAM",
        program.id,
        program.title,
        program.title + " " + program.content + " " + program.tags,
        terms
      );
    for (var team : teams.findAll())
      if (team.status.equals("OPEN") && Instant.now().isBefore(team.deadline)) add(
        result,
        "TEAM",
        team.id,
        team.title,
        team.title + " " + team.content + " " + team.tags + " " + team.roles,
        terms
      );
    return result
      .stream()
      .sorted(
        Comparator.comparingInt(Recommendation::score).reversed().thenComparing(Recommendation::id)
      )
      .limit(20)
      .toList();
  }

  private void add(
    List<Recommendation> out,
    String type,
    Long id,
    String title,
    String text,
    List<String> terms
  ) {
    var matches = terms
      .stream()
      .filter(t -> text.toLowerCase(Locale.ROOT).contains(t.toLowerCase(Locale.ROOT)))
      .toList();
    if (!matches.isEmpty()) out.add(
      new Recommendation(
        type,
        id,
        title,
        (type.equals("PROGRAM") ? "/programs/" : "/teams/") + id,
        "관심 키워드 일치: " + String.join(", ", matches),
        matches.size()
      )
    );
  }
}
