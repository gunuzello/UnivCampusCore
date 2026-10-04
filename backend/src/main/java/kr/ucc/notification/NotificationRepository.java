package kr.ucc.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
}
