package kr.ucc.notification;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@RestController @RequestMapping("/api/v1/notifications") public class NotificationController {
 private final NotificationRepository repository;public NotificationController(NotificationRepository repository){this.repository=repository;}
 @GetMapping List<Notification.View> list(Authentication a){return repository.findTop100ByUserIdOrderByCreatedAtDesc(CurrentUser.id(a)).stream().map(Notification::view).toList();}
 @PatchMapping("/{id}") @Transactional Notification.View read(@PathVariable Long id,Authentication a){var n=repository.findById(id).orElseThrow(ApiException::missing);if(n.userId!=CurrentUser.id(a))throw ApiException.forbidden();n.read=true;return n.view();}
}
