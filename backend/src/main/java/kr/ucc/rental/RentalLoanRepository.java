package kr.ucc.rental;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface RentalLoanRepository extends JpaRepository<RentalLoan, Long> {
  @Query("select l.itemId from RentalLoan l where l.id=:id")
  Optional<Long> itemId(Long id);

  List<RentalLoan> findByItemIdInOrderByRequestedAtDesc(List<Long> ids);
  List<RentalLoan> findByUserIdOrderByRequestedAtDesc(Long userId);
  boolean existsByItemIdAndUserIdAndStatusIn(
    Long itemId,
    Long userId,
    List<RentalLoan.Status> statuses
  );

  @Query(
    "select coalesce(sum(l.quantity),0) from RentalLoan l where l.itemId=:id and l.status in (kr.ucc.rental.RentalLoan.Status.REQUESTED,kr.ucc.rental.RentalLoan.Status.BORROWED)"
  )
  long reserved(Long id);
}
