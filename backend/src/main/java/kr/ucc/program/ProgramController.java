package kr.ucc.program;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.organization.OrganizationAccess;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Transactional
public class ProgramController {

  private final ProgramRepository programs;
  private final OrganizationAccess access;

  public ProgramController(ProgramRepository programs, OrganizationAccess access) {
    this.programs = programs;
    this.access = access;
  }

  public record Input(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 30000) String content,
    @NotBlank @Size(max = 80) String category,
    @Size(max = 500) String tags,
    @Size(max = 2000) String applicationUrl,
    @NotNull Instant deadline,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    boolean published
  ) {}

  public record View(
    Long id,
    Long organizationId,
    String title,
    String content,
    String category,
    String tags,
    String applicationUrl,
    Instant deadline,
    Instant startsAt,
    Instant endsAt,
    boolean published,
    boolean canManage
  ) {}

  private boolean manage(Program p, Long user) {
    try {
      access.staff(p.organizationId, user);
      return true;
    } catch (ApiException e) {
      return false;
    }
  }

  private View view(Program p, Long user) {
    return new View(
      p.id,
      p.organizationId,
      p.title,
      p.content,
      p.category,
      p.tags,
      p.applicationUrl,
      p.deadline,
      p.startsAt,
      p.endsAt,
      p.published,
      manage(p, user)
    );
  }

  private void set(Program p, Input d) {
    if (
      !d.startsAt().isBefore(d.endsAt()) || d.deadline().isAfter(d.endsAt())
    ) throw ApiException.bad("INVALID_PERIOD", "마감 및 진행 기간을 확인해 주세요.");
    if (d.applicationUrl() != null && !d.applicationUrl().isBlank()) try {
      var u = URI.create(d.applicationUrl());
      if (
        !Set.of("http", "https").contains(u.getScheme()) ||
        u.getHost() == null ||
        u.getUserInfo() != null
      ) throw new IllegalArgumentException();
    } catch (Exception e) {
      throw ApiException.bad("INVALID_URL", "신청 주소는 http 또는 https 주소여야 합니다.");
    }
    p.title = d.title().strip();
    p.content = d.content();
    p.category = d.category().strip();
    p.tags = d.tags();
    p.applicationUrl = d.applicationUrl();
    p.deadline = d.deadline();
    p.startsAt = d.startsAt();
    p.endsAt = d.endsAt();
    p.published = d.published();
  }

  @GetMapping("/programs")
  @Transactional(readOnly = true)
  List<View> list(
    @RequestParam(defaultValue = "") String search,
    @RequestParam(defaultValue = "") String category,
    @RequestParam(defaultValue = "false") boolean openOnly,
    Authentication a
  ) {
    Long user = CurrentUser.id(a);
    String q = search.toLowerCase(Locale.ROOT);
    return programs
      .findAll()
      .stream()
      .filter(p -> p.published || manage(p, user))
      .filter(p -> category.isBlank() || p.category.equals(category))
      .filter(p -> !openOnly || Instant.now().isBefore(p.deadline))
      .filter(p -> (p.title + " " + p.content + " " + p.tags).toLowerCase(Locale.ROOT).contains(q))
      .sorted(Comparator.comparing((Program p) -> p.deadline))
      .map(p -> view(p, user))
      .toList();
  }

  @GetMapping("/programs/{id}")
  View get(@PathVariable Long id, Authentication a) {
    var p = programs.findById(id).orElseThrow(ApiException::missing);
    Long user = CurrentUser.id(a);
    if (!p.published && !manage(p, user)) throw ApiException.missing();
    return view(p, user);
  }

  @PostMapping("/organizations/{org}/programs")
  View create(@PathVariable Long org, @Valid @RequestBody Input d, Authentication a) {
    Long user = CurrentUser.id(a);
    access.staff(org, user);
    var p = new Program();
    p.organizationId = org;
    set(p, d);
    return view(programs.save(p), user);
  }

  @PatchMapping("/programs/{id}")
  View update(@PathVariable Long id, @Valid @RequestBody Input d, Authentication a) {
    var p = programs.findById(id).orElseThrow(ApiException::missing);
    Long user = CurrentUser.id(a);
    access.staff(p.organizationId, user);
    set(p, d);
    return view(p, user);
  }
}
