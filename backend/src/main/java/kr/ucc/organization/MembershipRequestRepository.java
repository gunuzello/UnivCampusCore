package kr.ucc.organization;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRequestRepository extends JpaRepository<MembershipRequest, Long> {
  List<MembershipRequest> findByOrganizationIdOrderByCreatedAtDesc(Long org);
  Optional<MembershipRequest> findFirstByOrganizationIdAndUserIdOrderByCreatedAtDescIdDesc(
    Long org,
    Long user
  );
  boolean existsByOrganizationIdAndUserIdAndStatus(
    Long org,
    Long user,
    MembershipRequest.Status status
  );
}
