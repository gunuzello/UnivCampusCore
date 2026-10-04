package kr.ucc.schedule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
  List<Schedule> findByOrganizationIdOrderByStartsAtAsc(Long organizationId);
}
