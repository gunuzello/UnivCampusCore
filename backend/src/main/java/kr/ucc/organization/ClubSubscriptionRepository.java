package kr.ucc.organization;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubSubscriptionRepository extends JpaRepository<ClubSubscription, Long> {
  List<ClubSubscription> findByOrganizationId(Long organizationId);
  Optional<ClubSubscription> findByOrganizationIdAndUserId(Long organizationId, Long userId);
}
