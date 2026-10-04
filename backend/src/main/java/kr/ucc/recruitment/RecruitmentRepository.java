package kr.ucc.recruitment;
import org.springframework.data.jpa.repository.*;import jakarta.persistence.LockModeType;import java.util.*;
public interface RecruitmentRepository extends JpaRepository<Recruitment,Long>{
 List<Recruitment> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from Recruitment e where e.id=:id") Optional<Recruitment> lockById(Long id);
}
