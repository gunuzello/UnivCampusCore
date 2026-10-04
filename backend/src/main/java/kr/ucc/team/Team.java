package kr.ucc.team;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "teams")
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long ownerId;
  public String title;

  @Column(columnDefinition = "text")
  public String content;

  public String roles;
  public String tags;
  public int capacity;
  public Instant deadline;
  public String status = "OPEN";
}
