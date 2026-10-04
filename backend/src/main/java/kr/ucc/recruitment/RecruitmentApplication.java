package kr.ucc.recruitment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(
  name = "recruitment_applications",
  uniqueConstraints = @UniqueConstraint(columnNames = { "recruitment_id", "user_id" })
)
public class RecruitmentApplication {

  public enum Status {
    SUBMITTED,
    REVIEWING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "recruitment_id", nullable = false)
  public Long recruitmentId;

  @Column(name = "user_id", nullable = false)
  public Long userId;

  public String name;
  public String email;
  public String department;
  public String studentNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Status status = Status.SUBMITTED;

  @ElementCollection
  @CollectionTable(name = "recruitment_answers", joinColumns = @JoinColumn(name = "application_id"))
  @OrderColumn(name = "position")
  @Column(name = "answer", columnDefinition = "text")
  public List<String> answers = new ArrayList<>();

  public Instant submittedAt = Instant.now();

  protected RecruitmentApplication() {}
}
