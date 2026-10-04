package kr.ucc.organization;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "club_details")
public class ClubDetails {

  @Id
  public Long organizationId;

  public String meetingCycle = "";
  public int entryFee;
  public String photoUrl = "";
  public String recruitmentMode = "ALWAYS";
  public Instant opensAt;
  public Instant closesAt;

  public boolean open() {
    return (
      recruitmentMode.equals("ALWAYS") ||
      (recruitmentMode.equals("PERIOD") &&
        opensAt != null &&
        closesAt != null &&
        !Instant.now().isBefore(opensAt) &&
        Instant.now().isBefore(closesAt))
    );
  }
}
