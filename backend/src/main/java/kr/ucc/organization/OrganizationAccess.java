package kr.ucc.organization;

import kr.ucc.common.ApiException;
import org.springframework.stereotype.Service;

@Service
public class OrganizationAccess {

  private final MembershipRepository memberships;

  public OrganizationAccess(MembershipRepository memberships) {
    this.memberships = memberships;
  }

  public Membership member(Long organizationId, Long userId) {
    return memberships
      .findByOrganizationIdAndUserId(organizationId, userId)
      .orElseThrow(ApiException::forbidden);
  }

  public Membership staff(Long organizationId, Long userId) {
    var m = member(organizationId, userId);
    if (m.role == Membership.Role.MEMBER) throw ApiException.forbidden();
    return m;
  }

  public Membership leader(Long organizationId, Long userId) {
    var m = member(organizationId, userId);
    if (m.role != Membership.Role.LEADER) throw ApiException.forbidden();
    return m;
  }
}
