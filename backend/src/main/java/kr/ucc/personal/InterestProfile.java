package kr.ucc.personal;

import jakarta.persistence.*;

@Entity
@Table(name = "interest_profiles")
public class InterestProfile {

  @Id
  public Long userId;

  public String interests = "";
  public String activities = "";
  public String courses = "";
  public String skills = "";

  @Column(columnDefinition = "text")
  public String portfolio = "";
}
