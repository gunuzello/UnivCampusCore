package kr.ucc.team;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "team_applications",
  uniqueConstraints = @UniqueConstraint(columnNames = { "team_id", "user_id" })
)
public class TeamApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long teamId;
  public Long userId;
  public String role;

  @Column(columnDefinition = "text")
  public String message;

  public String status = "PENDING";
  public Instant updatedAt = Instant.now();
}
