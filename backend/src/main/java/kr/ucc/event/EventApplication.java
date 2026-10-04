package kr.ucc.event;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(
  name = "event_applications",
  uniqueConstraints = @UniqueConstraint(columnNames = { "event_id", "user_id" })
)
public class EventApplication {

  public enum Status {
    REGISTERED,
    ATTENDED,
    ABSENT,
    CANCELLED,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "event_id", nullable = false)
  public Long eventId;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  public String name;
  public String email;
  public String department;
  public String studentNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Status status = Status.REGISTERED;

  @ElementCollection
  @CollectionTable(name = "event_answers", joinColumns = @JoinColumn(name = "application_id"))
  @OrderColumn(name = "position")
  @Column(name = "answer", columnDefinition = "text")
  public List<String> answers = new ArrayList<>();

  public Instant submittedAt = Instant.now();

  protected EventApplication() {}
}
