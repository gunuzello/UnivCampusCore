package kr.ucc.rental;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rental_loans")
public class RentalLoan {

  public enum Status {
    REQUESTED,
    BORROWED,
    RETURNED,
    CANCELLED,
    REJECTED,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long itemId;
  public Long userId;
  public int quantity;

  @Enumerated(EnumType.STRING)
  public Status status;

  public String name, email, department, studentNumber;
  public Instant requestedAt = Instant.now(),
    borrowedAt,
    dueAt,
    returnedAt;

  protected RentalLoan() {}
}
