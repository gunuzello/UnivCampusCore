package kr.ucc.notification;
import org.springframework.stereotype.Service;
@Service public class Notifications {
 private final NotificationRepository repository;public Notifications(NotificationRepository repository){this.repository=repository;}
 public void send(Long userId,String message,String path){repository.save(new Notification(userId,message,path));}
}
