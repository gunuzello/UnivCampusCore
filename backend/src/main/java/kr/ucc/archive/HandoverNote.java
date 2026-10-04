package kr.ucc.archive;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "handover_notes")
public class HandoverNote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long organizationId;
  public String title;
  public String period;

  @Column(columnDefinition = "text")
  public String content;

  public Instant updatedAt = Instant.now();

  protected HandoverNote() {}

  public HandoverNote(Long organizationId, String title, String period, String content) {
    this.organizationId = organizationId;
    this.title = title;
    this.period = period;
    this.content = content;
  }

  public record View(Long id, String title, String period, String content, Instant updatedAt) {}

  public View view() {
    return new View(id, title, period, content, updatedAt);
  }
}
