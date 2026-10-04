package kr.ucc.organization;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Organization o where o.id=:id")
  Optional<Organization> lockById(Long id);
}
