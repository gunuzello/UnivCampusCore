package kr.ucc.team;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {
  List<TeamApplication> findByTeamId(Long teamId);
  Optional<TeamApplication> findByTeamIdAndUserId(Long teamId, Long userId);
  List<TeamApplication> findByUserIdAndStatus(Long userId, String status);
}
