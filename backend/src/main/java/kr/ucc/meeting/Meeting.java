package kr.ucc.meeting;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "meetings")
public class Meeting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long organizationId;

  @Column(nullable = false)
  public String title;

  public Instant startsAt;
  public Instant endsAt;

  @Column(columnDefinition = "text")
  public String content;

  @ElementCollection
  @CollectionTable(name = "meeting_attendees", joinColumns = @JoinColumn(name = "meeting_id"))
  @Column(name = "user_id")
  public Set<Long> attendees = new HashSet<>();

  @ElementCollection
  @CollectionTable(name = "meeting_agendas", joinColumns = @JoinColumn(name = "meeting_id"))
  @OrderColumn(name = "position")
  @Column(name = "content", columnDefinition = "text")
  public List<String> agendas = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "meeting_decisions", joinColumns = @JoinColumn(name = "meeting_id"))
  @OrderColumn(name = "position")
  @Column(name = "content", columnDefinition = "text")
  public List<String> decisions = new ArrayList<>();

  protected Meeting() {}
}
