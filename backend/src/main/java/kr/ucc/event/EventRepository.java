package kr.ucc.event;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EventRepository extends JpaRepository<Event, Long> {
  List<Event> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Event e where e.id=:id")
  Optional<Event> lockById(Long id);
}
