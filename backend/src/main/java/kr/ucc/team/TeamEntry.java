package kr.ucc.team;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team_entries")
public class TeamEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long teamId;
  public String kind;
  public String title;

  @Column(columnDefinition = "text")
  public String content;

  public Long assigneeId;
  public Instant startsAt;
  public Instant endsAt;
  public String url;
  public boolean done;
}
