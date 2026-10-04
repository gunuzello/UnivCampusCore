package kr.ucc.organization;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organization_notices")
public class OrganizationNotice {

  public enum Visibility {
    PUBLIC,
    MEMBERS,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long organizationId;
  public String title;

  @Column(columnDefinition = "text")
  public String content;

  @Enumerated(EnumType.STRING)
  public Visibility visibility = Visibility.MEMBERS;

  public Instant createdAt = Instant.now(),
    updatedAt = Instant.now();

  protected OrganizationNotice() {}
}
