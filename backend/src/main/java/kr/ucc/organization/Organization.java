package kr.ucc.organization;

import jakarta.persistence.*;

@Entity
@Table(name = "organizations")
public class Organization {

  public enum Type {
    STUDENT_COUNCIL,
    CLUB,
  }

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Type type = Type.STUDENT_COUNCIL;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public String name;

  public String department;

  @Column(columnDefinition = "text")
  public String description;

  protected Organization() {}

  public Organization(String name, String department, String description) {
    this.name = name;
    this.department = department;
    this.description = description;
  }
}
