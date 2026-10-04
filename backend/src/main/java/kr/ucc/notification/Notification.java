package kr.ucc.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long userId;

  @Column(nullable = false)
  public String message;

  public String path;

  @Column(nullable = false)
  public boolean read;

  @Column(nullable = false)
  public Instant createdAt;

  protected Notification() {}

  public Notification(Long userId, String message, String path) {
    this.userId = userId;
    this.message = message;
    this.path = path;
    this.createdAt = Instant.now();
  }

  public record View(Long id, String message, String path, boolean read, Instant createdAt) {}

  public View view() {
    return new View(id, message, path, read, createdAt);
  }
}
