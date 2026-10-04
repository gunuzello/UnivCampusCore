package kr.ucc.meeting;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "meeting_responses",
  uniqueConstraints = @UniqueConstraint(columnNames = { "meeting_id", "user_id" })
)
public class MeetingResponse {

  public enum Status {
    GOING,
    NOT_GOING,
    UNDECIDED,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long meetingId;

  @Column(nullable = false)
  public Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Status status;

  @Column(nullable = false)
  public Instant updatedAt = Instant.now();
}
