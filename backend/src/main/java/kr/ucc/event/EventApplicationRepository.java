package kr.ucc.event;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventApplicationRepository extends JpaRepository<EventApplication, Long> {
  Optional<EventApplication> findByEventIdAndUserId(Long eventId, Long userId);
  List<EventApplication> findByEventIdOrderBySubmittedAtAsc(Long eventId);
  List<EventApplication> findByUserIdOrderBySubmittedAtDesc(Long userId);
  long countByEventIdAndStatusNot(Long eventId, EventApplication.Status status);
}
