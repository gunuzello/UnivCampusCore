package kr.ucc.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{org}/club-details")
@Transactional
public class ClubDetailsController {

  private final ClubDetailsRepository details;
  private final ClubSubscriptionRepository subscriptions;
  private final OrganizationRepository organizations;
  private final OrganizationAccess access;
  private final ClubRecruitmentService recruitment;
  private final MembershipRepository memberships;

  public ClubDetailsController(
    ClubDetailsRepository details,
    ClubSubscriptionRepository subscriptions,
    OrganizationRepository organizations,
    OrganizationAccess access,
    ClubRecruitmentService recruitment,
    MembershipRepository memberships
  ) {
    this.details = details;
    this.subscriptions = subscriptions;
    this.organizations = organizations;
    this.access = access;
    this.recruitment = recruitment;
    this.memberships = memberships;
  }

  public record Input(
    @Size(max = 2000) String meetingCycle,
    @Min(0) @Max(10000000) int entryFee,
    @Size(max = 2000) String photoUrl,
    @NotBlank String recruitmentMode,
    Instant opensAt,
    Instant closesAt
  ) {}

  public record View(ClubDetails details, boolean open, boolean subscribed, int memberCount) {}

  private Organization org(Long id) {
    var o = organizations.findById(id).orElseThrow(ApiException::missing);
    if (o.type != Organization.Type.CLUB) throw ApiException.missing();
    return o;
  }

  private View view(Long org, Long user) {
    var d = details.findById(org).orElseGet(ClubDetails::new);
    return new View(
      d,
      d.open(),
      subscriptions.findByOrganizationIdAndUserId(org, user).isPresent(),
      memberships.findByOrganizationId(org).size()
    );
  }

  @GetMapping
  View get(@PathVariable Long org, Authentication a) {
    org(org);
    return view(org, CurrentUser.id(a));
  }

  @PutMapping
  View update(@PathVariable Long org, @Valid @RequestBody Input input, Authentication a) {
    organizations.lockById(org).orElseThrow(ApiException::missing);
    var o = org(org);
    Long u = CurrentUser.id(a);
    access.staff(org, u);
    if (
      !Set.of("ALWAYS", "PERIOD", "CLOSED").contains(input.recruitmentMode())
    ) throw ApiException.bad("INVALID_MODE", "모집 방식을 확인해 주세요.");
    if (
      input.recruitmentMode().equals("PERIOD") &&
      (input.opensAt() == null ||
        input.closesAt() == null ||
        !input.opensAt().isBefore(input.closesAt()))
    ) throw ApiException.bad("INVALID_PERIOD", "모집 시작과 종료를 확인해 주세요.");
    if (input.photoUrl() != null && !input.photoUrl().isBlank()) try {
      var uri = URI.create(input.photoUrl());
      if (
        !Set.of("http", "https").contains(uri.getScheme()) ||
        uri.getHost() == null ||
        uri.getUserInfo() != null
      ) throw new IllegalArgumentException();
    } catch (Exception e) {
      throw ApiException.bad("INVALID_URL", "사진은 http 또는 https 주소를 입력해 주세요.");
    }
    var d = details.findById(org).orElseGet(ClubDetails::new);
    boolean reset =
      !d.recruitmentMode.equals(input.recruitmentMode()) ||
      !Objects.equals(d.opensAt, input.opensAt());
    d.organizationId = org;
    d.meetingCycle = input.meetingCycle();
    d.entryFee = input.entryFee();
    d.photoUrl = input.photoUrl();
    d.recruitmentMode = input.recruitmentMode();
    d.opensAt = input.opensAt();
    d.closesAt = input.closesAt();
    details.save(d);
    if (reset) for (var s : subscriptions.findByOrganizationId(org)) s.notified = false;
    recruitment.notifyOpen(o, d);
    return view(org, u);
  }

  @PutMapping("/subscription")
  View subscribe(@PathVariable Long org, Authentication a) {
    organizations.lockById(org).orElseThrow(ApiException::missing);
    org(org);
    Long u = CurrentUser.id(a);
    var d = details.findById(org).orElseGet(ClubDetails::new);
    if (d.open()) throw ApiException.bad(
      "ALREADY_OPEN",
      "현재 모집 중입니다. 가입 신청을 이용해 주세요."
    );
    if (subscriptions.findByOrganizationIdAndUserId(org, u).isEmpty()) {
      var s = new ClubSubscription();
      s.organizationId = org;
      s.userId = u;
      subscriptions.save(s);
    }
    return view(org, u);
  }

  @DeleteMapping("/subscription")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void unsubscribe(@PathVariable Long org, Authentication a) {
    organizations.lockById(org).orElseThrow(ApiException::missing);
    org(org);
    subscriptions
      .findByOrganizationIdAndUserId(org, CurrentUser.id(a))
      .ifPresent(subscriptions::delete);
  }
}
