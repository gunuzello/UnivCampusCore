package kr.ucc.organization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationWorkRepository extends JpaRepository<OrganizationWork, Long> {
  List<OrganizationWork> findByOrganizationIdOrderByIdDesc(Long organizationId);
}
