package kr.ucc.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MembershipRepository extends JpaRepository<Membership,Long>{
 Optional<Membership> findByOrganizationIdAndUserId(Long organizationId,Long userId);
 List<Membership> findByOrganizationId(Long organizationId);
 long countByOrganizationIdAndRole(Long organizationId,Membership.Role role);
}
