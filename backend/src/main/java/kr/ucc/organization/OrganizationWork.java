package kr.ucc.organization;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organization_work")
public class OrganizationWork {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long organizationId;
  public Long creatorId;
  public Long assigneeId;
  public String kind;
  public String title;

  @Column(columnDefinition = "text")
  public String content;

  public String requestedRole;
  public String status = "PENDING";

  @Column(columnDefinition = "text")
  public String response;

  public Instant dueAt;
  public Instant updatedAt = Instant.now();
}
