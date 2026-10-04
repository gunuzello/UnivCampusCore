package kr.ucc.archive;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {
  List<ExternalLink> findByOrganizationIdAndTargetTypeAndTargetId(
    Long organizationId,
    String targetType,
    Long targetId
  );
}
