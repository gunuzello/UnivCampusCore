package kr.ucc.organization;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface OrganizationRepository extends JpaRepository<Organization,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from Organization o where o.id=:id") Optional<Organization> lockById(Long id);
}
