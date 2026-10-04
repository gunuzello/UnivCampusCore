package kr.ucc.schedule;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "schedules")
public class Schedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long organizationId;

  public String title;
  public Instant startsAt;
  public Instant endsAt;

  @Column(columnDefinition = "text")
  public String description;

  protected Schedule() {}

  public Schedule(
    Long organizationId,
    String title,
    Instant startsAt,
    Instant endsAt,
    String description
  ) {
    this.organizationId = organizationId;
    this.title = title;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.description = description;
  }
}
