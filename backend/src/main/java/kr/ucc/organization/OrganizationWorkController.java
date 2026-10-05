package kr.ucc.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
@RequestMapping("/api/v1/organizations/{org}/work")
@Transactional
public class OrganizationWorkController {

  private final OrganizationRepository organizations;
  private final OrganizationWorkRepository work;
  private final OrganizationAccess access;
  private final MembershipRepository memberships;
  private final UserRepository users;
  private final Notifications notifications;

  public OrganizationWorkController(
    OrganizationRepository organizations,
    OrganizationWorkRepository work,
    OrganizationAccess access,
    MembershipRepository memberships,
    UserRepository users,
    Notifications notifications
  ) {
    this.organizations = organizations;
    this.work = work;
    this.access = access;
    this.memberships = memberships;
    this.users = users;
    this.notifications = notifications;
  }

  public record Input(
    @NotBlank String kind,
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 10000) String content,
    String requestedRole,
    Long assigneeId,
    Instant dueAt
  ) {}

  public record Decision(@NotBlank String status, @Size(max = 10000) String response) {}

  public record View(
    Long id,
    String kind,
    String title,
    String content,
    String requestedRole,
    String status,
    String response,
    Long creatorId,
    String creatorName,
    Long assigneeId,
    String assigneeName,
    Instant dueAt,
    Instant updatedAt,
    boolean canManage,
    boolean canComplete,
    boolean canCancel
  ) {}

  private boolean staff(Long org, Long u) {
    return memberships
      .findByOrganizationIdAndUserId(org, u)
      .filter(m -> m.role != Membership.Role.MEMBER)
      .isPresent();
  }

  private boolean leader(Long org, Long u) {
    return memberships
      .findByOrganizationIdAndUserId(org, u)
      .filter(m -> m.role == Membership.Role.LEADER)
      .isPresent();
  }

  private void lock(Long org) {
    organizations.lockById(org).orElseThrow(ApiException::missing);
  }

  private View view(OrganizationWork w, Long u) {
    return new View(
      w.id,
      w.kind,
      w.title,
      w.content,
      w.requestedRole,
      w.status,
      w.response,
      w.creatorId,
      users.findById(w.creatorId).orElseThrow().name,
      w.assigneeId,
      w.assigneeId == null ? null : users.findById(w.assigneeId).orElseThrow().name,
      w.dueAt,
      w.updatedAt,
      w.kind.equals("ROLE") ? leader(w.organizationId, u) : staff(w.organizationId, u),
      w.kind.equals("TASK") && (staff(w.organizationId, u) || u.equals(w.assigneeId)),
      !w.kind.equals("TASK") && w.creatorId.equals(u) && w.status.equals("PENDING")
    );
  }

  @GetMapping
  List<View> list(@PathVariable Long org, Authentication a) {
    if (!organizations.existsById(org)) throw ApiException.missing();
    Long u = CurrentUser.id(a);
    boolean member = memberships.findByOrganizationIdAndUserId(org, u).isPresent();
    return work
      .findByOrganizationIdOrderByIdDesc(org)
      .stream()
      .filter(
        w ->
          switch (w.kind) {
            case "TASK" -> member;
            case "ROLE" -> w.creatorId.equals(u) || leader(org, u);
            default -> w.creatorId.equals(u) || staff(org, u);
          }
      )
      .map(w -> view(w, u))
      .toList();
  }

  private void set(OrganizationWork w, Input d) {
    w.title = d.title().strip();
    w.content = d.content();
    w.dueAt = d.dueAt();
    w.assigneeId = d.assigneeId();
    if (w.assigneeId != null) access.member(w.organizationId, w.assigneeId);
    w.updatedAt = Instant.now();
  }

  @PostMapping
  View create(@PathVariable Long org, @Valid @RequestBody Input d, Authentication a) {
    lock(org);
    Long u = CurrentUser.id(a);
    if (!Set.of("ROLE", "SUGGESTION", "TASK").contains(d.kind())) throw ApiException.bad(
      "INVALID_KIND",
      "유형을 확인해 주세요."
    );
    var w = new OrganizationWork();
    w.organizationId = org;
    w.creatorId = u;
    w.kind = d.kind();
    if (w.kind.equals("ROLE")) {
      var m = access.member(org, u);
      if (
        !Set.of("STAFF", "LEADER").contains(Objects.toString(d.requestedRole(), "")) ||
        m.role.name().equals(d.requestedRole()) ||
        m.role == Membership.Role.LEADER
      ) throw ApiException.bad("INVALID_ROLE", "신청할 역할을 확인해 주세요.");
      if (
        work
          .findByOrganizationIdOrderByIdDesc(org)
          .stream()
          .anyMatch(
            r -> r.kind.equals("ROLE") && r.creatorId.equals(u) && r.status.equals("PENDING")
          )
      ) throw ApiException.bad("DUPLICATE", "검토 중인 권한 신청이 있습니다.");
      w.requestedRole = d.requestedRole();
    }
    if (w.kind.equals("TASK")) access.staff(org, u);
    set(
      w,
      new Input(
        d.kind(),
        d.title(),
        d.content(),
        d.requestedRole(),
        w.kind.equals("TASK") ? d.assigneeId() : null,
        w.kind.equals("TASK") ? d.dueAt() : null
      )
    );
    work.save(w);
    for (var m : memberships.findByOrganizationId(org))
      if (
        w.kind.equals("ROLE")
          ? m.role == Membership.Role.LEADER
          : w.kind.equals("SUGGESTION")
            ? m.role != Membership.Role.MEMBER
            : Objects.equals(m.userId, w.assigneeId)
      ) notifications.send(m.userId, "새 소속 요청/업무: " + w.title, "/organization?org=" + org);
    return view(w, u);
  }

  @PatchMapping("/{id}")
  View updateTask(
    @PathVariable Long org,
    @PathVariable Long id,
    @Valid @RequestBody Input d,
    Authentication a
  ) {
    lock(org);
    Long u = CurrentUser.id(a);
    access.staff(org, u);
    var w = get(org, id);
    if (!w.kind.equals("TASK") || !d.kind().equals("TASK")) throw ApiException.bad(
      "INVALID_KIND",
      "업무만 수정할 수 있습니다."
    );
    set(w, d);
    return view(w, u);
  }

  private OrganizationWork get(Long org, Long id) {
    var w = work.findById(id).orElseThrow(ApiException::missing);
    if (!w.organizationId.equals(org)) throw ApiException.missing();
    return w;
  }

  @PatchMapping("/{id}/status")
  View decide(
    @PathVariable Long org,
    @PathVariable Long id,
    @Valid @RequestBody Decision d,
    Authentication a
  ) {
    lock(org);
    Long u = CurrentUser.id(a);
    var w = get(org, id);
    if (w.kind.equals("TASK")) {
      access.member(org, u);
      if (!staff(org, u) && !u.equals(w.assigneeId)) throw ApiException.forbidden();
      if (!Set.of("PENDING", "DONE").contains(d.status())) throw ApiException.bad(
        "INVALID_STATUS",
        "업무 상태를 확인해 주세요."
      );
    } else if (d.status().equals("CANCELLED")) {
      if (!w.creatorId.equals(u)) throw ApiException.forbidden();
      if (!w.status.equals("PENDING")) throw ApiException.bad(
        "INVALID_STATUS",
        "처리 전 요청만 취소할 수 있습니다."
      );
    } else {
      if (w.kind.equals("ROLE")) {
        access.leader(org, u);
        if (!Set.of("ACCEPTED", "REJECTED").contains(d.status())) throw ApiException.bad(
          "INVALID_STATUS",
          "승인 또는 반려를 선택해 주세요."
        );
        if (!w.status.equals("PENDING")) throw ApiException.bad(
          "INVALID_STATUS",
          "처리된 요청입니다."
        );
        if (d.status().equals("ACCEPTED")) {
          var member = access.member(org, w.creatorId);
          var requestedRole = Membership.Role.valueOf(w.requestedRole);
          if (
            member.role == Membership.Role.LEADER &&
            requestedRole != Membership.Role.LEADER &&
            memberships.countByOrganizationIdAndRole(org, Membership.Role.LEADER) <= 1
          ) throw ApiException.bad(
            "LAST_LEADER",
            "마지막 대표의 권한은 낮출 수 없습니다. 변경된 역할을 확인하고 신청을 반려해 주세요."
          );
          if (
            member.role == requestedRole || member.role == Membership.Role.LEADER
          ) throw ApiException.bad(
            "ROLE_CHANGED",
            "신청 이후 구성원의 역할이 변경되었습니다. 현재 역할을 확인하고 신청을 반려해 주세요."
          );
          member.role = requestedRole;
        }
      } else {
        access.staff(org, u);
        if (
          !d.status().equals("ANSWERED") || d.response() == null || d.response().isBlank()
        ) throw ApiException.bad("INVALID_RESPONSE", "건의 답변이 필요합니다.");
      }
      if (
        !w.status.equals("PENDING") && !(w.kind.equals("SUGGESTION") && w.status.equals("ANSWERED"))
      ) throw ApiException.bad("INVALID_STATUS", "처리된 요청입니다.");
      w.response = d.response();
    }
    w.status = d.status();
    w.updatedAt = Instant.now();
    notifications.send(w.creatorId, "소속 요청/업무 처리: " + w.title, "/organization?org=" + org);
    return view(w, u);
  }
}
