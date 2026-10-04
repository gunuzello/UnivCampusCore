package kr.ucc.team;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamEntryRepository extends JpaRepository<TeamEntry, Long> {
  List<TeamEntry> findByTeamIdOrderByIdDesc(Long teamId);
}
