package kr.ucc.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.notification.Notifications;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{org}/notices")
@Transactional
public class OrganizationNoticeController {

  private final OrganizationNoticeRepository notices;
  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final OrganizationAccess access;
  private final Notifications notifications;

  public OrganizationNoticeController(
    OrganizationNoticeRepository notices,
    OrganizationRepository organizations,
    MembershipRepository memberships,
    OrganizationAccess access,
    Notifications notifications
  ) {
    this.notices = notices;
    this.organizations = organizations;
    this.memberships = memberships;
    this.access = access;
    this.notifications = notifications;
  }

  public record Input(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 30000) String content,
    @NotNull OrganizationNotice.Visibility visibility
  ) {}

  public record View(
    Long id,
    String title,
    String content,
    OrganizationNotice.Visibility visibility,
    Instant createdAt,
    Instant updatedAt
  ) {}

  private View view(OrganizationNotice n) {
    return new View(n.id, n.title, n.content, n.visibility, n.createdAt, n.updatedAt);
  }

  @GetMapping
  @Transactional(readOnly = true)
  List<View> list(@PathVariable Long org, Authentication a) {
    if (!organizations.existsById(org)) throw ApiException.missing();
    boolean member = memberships.findByOrganizationIdAndUserId(org, CurrentUser.id(a)).isPresent();
    return notices
      .findByOrganizationIdOrderByUpdatedAtDescIdDesc(org)
      .stream()
      .filter(n -> member || n.visibility == OrganizationNotice.Visibility.PUBLIC)
      .map(this::view)
      .toList();
  }

  @PostMapping
  View create(@PathVariable Long org, @Valid @RequestBody Input input, Authentication a) {
    access.staff(org, CurrentUser.id(a));
    var notice = new OrganizationNotice();
    notice.organizationId = org;
    set(notice, input);
    notices.save(notice);
    notifyMembers(org, input.title());
    return view(notice);
  }

  @PatchMapping("/{id}")
  View update(
    @PathVariable Long org,
    @PathVariable Long id,
    @Valid @RequestBody Input input,
    Authentication a
  ) {
    access.staff(org, CurrentUser.id(a));
    var notice = notices.findById(id).orElseThrow(ApiException::missing);
    if (!notice.organizationId.equals(org)) throw ApiException.missing();
    set(notice, input);
    return view(notice);
  }

  private void set(OrganizationNotice notice, Input input) {
    notice.title = input.title().strip();
    notice.content = input.content();
    notice.visibility = input.visibility();
    notice.updatedAt = Instant.now();
  }

  private void notifyMembers(Long org, String title) {
    for (var m : memberships.findByOrganizationId(org))
      notifications.send(m.userId, "새 소속 소식: " + title, "/organization?org=" + org);
  }
}
