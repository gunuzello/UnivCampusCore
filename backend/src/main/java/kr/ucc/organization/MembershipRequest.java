package kr.ucc.organization;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "membership_requests")
public class MembershipRequest {

  public enum Status {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long organizationId, userId;

  @Column(length = 2000)
  public String message;

  @Enumerated(EnumType.STRING)
  public Status status = Status.PENDING;

  public Instant createdAt = Instant.now(),
    resolvedAt;

  protected MembershipRequest() {}
}
