package kr.ucc.organization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationNoticeRepository extends JpaRepository<OrganizationNotice, Long> {
  List<OrganizationNotice> findByOrganizationIdOrderByUpdatedAtDescIdDesc(Long organizationId);
}
