package kr.ucc.organization;

import jakarta.persistence.*;

@Entity
@Table(
  name = "club_subscriptions",
  uniqueConstraints = @UniqueConstraint(columnNames = { "organization_id", "user_id" })
)
public class ClubSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long organizationId;
  public Long userId;
  public boolean notified;
}
