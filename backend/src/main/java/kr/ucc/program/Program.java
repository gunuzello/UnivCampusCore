package kr.ucc.program;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "programs")
public class Program {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long organizationId;

  public String title;

  @Column(columnDefinition = "text")
  public String content;

  public String category;
  public String tags;
  public String applicationUrl;
  public Instant deadline;
  public Instant startsAt;
  public Instant endsAt;
  public boolean published;
}
