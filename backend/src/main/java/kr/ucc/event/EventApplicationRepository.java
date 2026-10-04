package kr.ucc.event;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface EventApplicationRepository extends JpaRepository<EventApplication,Long>{
 Optional<EventApplication> findByEventIdAndUserId(Long eventId,Long userId);
 List<EventApplication> findByEventIdOrderBySubmittedAtAsc(Long eventId);
 long countByEventIdAndStatusNot(Long eventId,EventApplication.Status status);
}
