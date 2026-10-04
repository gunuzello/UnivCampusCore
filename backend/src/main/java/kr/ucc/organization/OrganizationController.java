package kr.ucc.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final UserRepository users;
  private final OrganizationAccess access;

  public OrganizationController(
    OrganizationRepository organizations,
    MembershipRepository memberships,
    UserRepository users,
    OrganizationAccess access
  ) {
    this.organizations = organizations;
    this.memberships = memberships;
    this.users = users;
    this.access = access;
  }

  public record Input(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 120) String department,
    @Size(max = 10000) String description,
    Organization.Type type
  ) {}

  public record MemberInput(@NotBlank @Email String email, @NotNull Membership.Role role) {}

  public record RoleInput(@NotNull Membership.Role role) {}

  public record View(
    Long id,
    String name,
    String department,
    String description,
    Membership.Role role,
    Organization.Type type
  ) {}

  public record MemberView(Long id, User.Profile user, Membership.Role role) {}

  private View view(Organization o, long user) {
    return new View(
      o.id,
      o.name,
      o.department,
      o.description,
      memberships
        .findByOrganizationIdAndUserId(o.id, user)
        .map(m -> m.role)
        .orElse(null),
      o.type
    );
  }

  @GetMapping
  List<View> list(Authentication a) {
    return organizations
      .findAll()
      .stream()
      .map(o -> view(o, CurrentUser.id(a)))
      .toList();
  }

  @GetMapping("/{id}")
  View get(@PathVariable Long id, Authentication a) {
    return view(organizations.findById(id).orElseThrow(ApiException::missing), CurrentUser.id(a));
  }

  @PostMapping
  @Transactional
  @ResponseStatus(HttpStatus.CREATED)
  View create(@Valid @RequestBody Input d, Authentication a) {
    var entity = new Organization(d.name(), d.department(), d.description());
    if (d.type() != null) entity.type = d.type();
    var o = organizations.save(entity);
    memberships.save(new Membership(o.id, CurrentUser.id(a), Membership.Role.LEADER));
    return view(o, CurrentUser.id(a));
  }

  @PatchMapping("/{id}")
  @Transactional
  View update(@PathVariable Long id, @Valid @RequestBody Input d, Authentication a) {
    access.staff(id, CurrentUser.id(a));
    var o = organizations.findById(id).orElseThrow(ApiException::missing);
    if (d.type() != null && d.type() != o.type) throw ApiException.bad(
      "TYPE_IMMUTABLE",
      "소속 유형은 생성 후 변경할 수 없습니다."
    );
    o.name = d.name();
    o.department = d.department();
    o.description = d.description();
    return view(o, CurrentUser.id(a));
  }

  @GetMapping("/{id}/members")
  List<MemberView> members(@PathVariable Long id, Authentication a) {
    access.member(id, CurrentUser.id(a));
    return memberships
      .findByOrganizationId(id)
      .stream()
      .map(m ->
        new MemberView(
          m.id,
          users.findById(m.userId).orElseThrow(ApiException::missing).profile(),
          m.role
        )
      )
      .toList();
  }

  @PostMapping("/{id}/members")
  @Transactional
  MemberView add(@PathVariable Long id, @Valid @RequestBody MemberInput d, Authentication a) {
    organizations.lockById(id).orElseThrow(ApiException::missing);
    access.leader(id, CurrentUser.id(a));
    var u = users
      .findByEmail(d.email().trim().toLowerCase(Locale.ROOT))
      .orElseThrow(ApiException::missing);
    if (memberships.findByOrganizationIdAndUserId(id, u.id).isPresent()) throw ApiException.bad(
      "ALREADY_MEMBER",
      "이미 조직 구성원입니다."
    );
    var m = memberships.save(new Membership(id, u.id, d.role()));
    return new MemberView(m.id, u.profile(), m.role);
  }

  private Membership target(Long org, Long member) {
    var m = memberships.findById(member).orElseThrow(ApiException::missing);
    if (!m.organizationId.equals(org)) throw ApiException.missing();
    return m;
  }

  private void preserveLeader(Membership m) {
    if (
      m.role == Membership.Role.LEADER &&
      memberships.countByOrganizationIdAndRole(m.organizationId, Membership.Role.LEADER) <= 1
    ) throw ApiException.bad(
      "LAST_LEADER",
      "마지막 대표는 제거하거나 강등할 수 없습니다. 먼저 다른 대표를 지정해 주세요."
    );
  }

  @PatchMapping("/{id}/members/{memberId}")
  @Transactional
  MemberView role(
    @PathVariable Long id,
    @PathVariable Long memberId,
    @Valid @RequestBody RoleInput d,
    Authentication a
  ) {
    organizations.lockById(id).orElseThrow(ApiException::missing);
    access.leader(id, CurrentUser.id(a));
    var m = target(id, memberId);
    if (d.role() != Membership.Role.LEADER) preserveLeader(m);
    m.role = d.role();
    return new MemberView(
      m.id,
      users.findById(m.userId).orElseThrow(ApiException::missing).profile(),
      m.role
    );
  }

  @DeleteMapping("/{id}/members/{memberId}")
  @Transactional
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void remove(@PathVariable Long id, @PathVariable Long memberId, Authentication a) {
    organizations.lockById(id).orElseThrow(ApiException::missing);
    access.leader(id, CurrentUser.id(a));
    var m = target(id, memberId);
    preserveLeader(m);
    memberships.delete(m);
  }
}
