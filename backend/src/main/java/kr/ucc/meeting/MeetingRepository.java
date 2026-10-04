package kr.ucc.meeting;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from Meeting m where m.id = :id")
  Optional<Meeting> lockById(Long id);

  List<Meeting> findByOrganizationIdOrderByStartsAtDesc(Long organizationId);
}
