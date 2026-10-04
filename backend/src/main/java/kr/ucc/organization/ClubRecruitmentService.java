package kr.ucc.organization;

import kr.ucc.common.ApiException;
import kr.ucc.notification.Notifications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClubRecruitmentService {

  private final ClubDetailsRepository details;
  private final ClubSubscriptionRepository subscriptions;
  private final OrganizationRepository organizations;
  private final Notifications notifications;

  public ClubRecruitmentService(
    ClubDetailsRepository details,
    ClubSubscriptionRepository subscriptions,
    OrganizationRepository organizations,
    Notifications notifications
  ) {
    this.details = details;
    this.subscriptions = subscriptions;
    this.organizations = organizations;
    this.notifications = notifications;
  }

  public void check(Organization org) {
    if (
      org.type == Organization.Type.CLUB &&
      !details.findById(org.id).orElseGet(ClubDetails::new).open()
    ) throw ApiException.bad("RECRUITMENT_CLOSED", "현재 동아리 가입 신청 기간이 아닙니다.");
  }

  public void notifyOpen(Organization org, ClubDetails d) {
    if (d.open()) for (var s : subscriptions.findByOrganizationId(org.id))
      if (!s.notified) {
        notifications.send(
          s.userId,
          "동아리 모집 시작: " + org.name,
          "/organization?org=" + org.id
        );
        s.notified = true;
      }
  }

  @Scheduled(fixedDelay = 60000, initialDelay = 60000)
  @Transactional
  public void tick() {
    for (var source : organizations.findAll())
      if (source.type == Organization.Type.CLUB) {
        var org = organizations.lockById(source.id).orElseThrow(ApiException::missing);
        var d = details.findById(org.id).orElseGet(ClubDetails::new);
        notifyOpen(org, d);
      }
  }
}
