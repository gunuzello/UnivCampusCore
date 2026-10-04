package kr.ucc.personal;

import jakarta.persistence.*;

@Entity
@Table(
  name = "saved_activities",
  uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "type", "target_id" })
)
public class SavedActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long userId;
  public String type;
  public Long targetId;
}
