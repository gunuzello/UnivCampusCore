package kr.ucc.rental;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "rental_items")
public class RentalItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long organizationId;
  public String name;

  @Column(columnDefinition = "text")
  public String description;

  public int totalQuantity;
  public int loanDays;
  public boolean enabled;
  public Instant createdAt = Instant.now();

  protected RentalItem() {}
}
