package kr.ucc.organization;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
  Optional<Membership> findByOrganizationIdAndUserId(Long organizationId, Long userId);
  List<Membership> findByOrganizationId(Long organizationId);
  long countByOrganizationIdAndRole(Long organizationId, Membership.Role role);
}
