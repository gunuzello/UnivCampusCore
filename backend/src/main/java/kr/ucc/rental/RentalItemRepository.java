package kr.ucc.rental;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {
  List<RentalItem> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from RentalItem i where i.id=:id")
  Optional<RentalItem> lockById(Long id);
}
