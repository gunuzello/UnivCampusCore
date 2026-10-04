package kr.ucc.personal;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedActivityRepository extends JpaRepository<SavedActivity, Long> {
  List<SavedActivity> findByUserIdOrderByIdDesc(Long userId);
  Optional<SavedActivity> findByUserIdAndTypeAndTargetId(Long userId, String type, Long targetId);
}
