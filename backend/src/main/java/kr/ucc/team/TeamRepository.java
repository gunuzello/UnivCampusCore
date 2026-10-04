package kr.ucc.team;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface TeamRepository extends JpaRepository<Team, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Team t where t.id=:id")
  Optional<Team> lockById(Long id);
}
