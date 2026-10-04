package kr.ucc.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.notification.Notifications;
import kr.ucc.user.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{org}/membership-requests")
@Transactional
public class MembershipRequestController {

  private final MembershipRequestRepository requests;
  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final UserRepository users;
  private final OrganizationAccess access;
  private final Notifications notifications;

  public MembershipRequestController(
    MembershipRequestRepository requests,
    OrganizationRepository organizations,
    MembershipRepository memberships,
    UserRepository users,
    OrganizationAccess access,
    Notifications notifications
  ) {
    this.requests = requests;
    this.organizations = organizations;
    this.memberships = memberships;
    this.users = users;
    this.access = access;
    this.notifications = notifications;
  }

  public record Input(@Size(max = 2000) String message) {}

  public record Decision(@NotNull MembershipRequest.Status status) {}

  public record View(
    Long id,
    User.Profile user,
    String message,
    MembershipRequest.Status status,
    Instant createdAt,
    Instant resolvedAt
  ) {}

  private View view(MembershipRequest r) {
    return new View(
      r.id,
      users.findById(r.userId).orElseThrow(ApiException::missing).profile(),
      r.message,
      r.status,
      r.createdAt,
      r.resolvedAt
    );
  }

  @PostMapping
  View apply(@PathVariable Long org, @Valid @RequestBody Input d, Authentication a) {
    long user = CurrentUser.id(a);
    var organization = organizations.lockById(org).orElseThrow(ApiException::missing);
    if (memberships.findByOrganizationIdAndUserId(org, user).isPresent()) throw ApiException.bad(
      "ALREADY_MEMBER",
      "이미 소속 구성원입니다."
    );
    if (
      requests.existsByOrganizationIdAndUserIdAndStatus(org, user, MembershipRequest.Status.PENDING)
    ) throw ApiException.bad("ALREADY_REQUESTED", "이미 검토 중인 가입 신청이 있습니다.");
    var r = new MembershipRequest();
    r.organizationId = org;
    r.userId = user;
    r.message = d.message();
    requests.save(r);
    for (var member : memberships.findByOrganizationId(org))
      if (member.role == Membership.Role.LEADER) notifications.send(
        member.userId,
        organization.name + "에 새 가입 신청이 도착했습니다.",
        "/organization?org=" + org
      );
    notifications.send(
      user,
      organization.name + " 가입 신청을 접수했습니다.",
      "/organization?org=" + org
    );
    return view(r);
  }

  @GetMapping("/me")
  @Transactional(readOnly = true)
  ResponseEntity<View> mine(@PathVariable Long org, Authentication a) {
    if (!organizations.existsById(org)) throw ApiException.missing();
    return requests
      .findFirstByOrganizationIdAndUserIdOrderByCreatedAtDescIdDesc(org, CurrentUser.id(a))
      .map(r -> ResponseEntity.ok(view(r)))
      .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping
  @Transactional(readOnly = true)
  List<View> list(@PathVariable Long org, Authentication a) {
    access.leader(org, CurrentUser.id(a));
    return requests.findByOrganizationIdOrderByCreatedAtDesc(org).stream().map(this::view).toList();
  }

  private MembershipRequest target(Long org, Long id) {
    var r = requests.findById(id).orElseThrow(ApiException::missing);
    if (!r.organizationId.equals(org)) throw ApiException.missing();
    return r;
  }

  @PostMapping("/{id}/cancellation")
  View cancel(@PathVariable Long org, @PathVariable Long id, Authentication a) {
    organizations.lockById(org).orElseThrow(ApiException::missing);
    var r = target(org, id);
    if (!r.userId.equals(CurrentUser.id(a))) throw ApiException.forbidden();
    pending(r);
    r.status = MembershipRequest.Status.CANCELLED;
    r.resolvedAt = Instant.now();
    return view(r);
  }

  private void pending(MembershipRequest r) {
    if (r.status != MembershipRequest.Status.PENDING) throw ApiException.bad(
      "INVALID_STATE",
      "이미 처리된 가입 신청입니다."
    );
  }

  @PatchMapping("/{id}/status")
  View decide(
    @PathVariable Long org,
    @PathVariable Long id,
    @Valid @RequestBody Decision d,
    Authentication a
  ) {
    var organization = organizations.lockById(org).orElseThrow(ApiException::missing);
    access.leader(org, CurrentUser.id(a));
    var r = target(org, id);
    pending(r);
    if (
      d.status() != MembershipRequest.Status.ACCEPTED &&
      d.status() != MembershipRequest.Status.REJECTED
    ) throw ApiException.bad("INVALID_STATE", "승인 또는 반려를 선택해 주세요.");
    if (
      d.status() == MembershipRequest.Status.ACCEPTED &&
      memberships.findByOrganizationIdAndUserId(org, r.userId).isEmpty()
    ) memberships.save(new Membership(org, r.userId, Membership.Role.MEMBER));
    r.status = d.status();
    r.resolvedAt = Instant.now();
    notifications.send(
      r.userId,
      organization.name +
        " 가입 신청이 " +
        (r.status == MembershipRequest.Status.ACCEPTED ? "승인" : "반려") +
        "되었습니다.",
      "/organization?org=" + org
    );
    return view(r);
  }
}
