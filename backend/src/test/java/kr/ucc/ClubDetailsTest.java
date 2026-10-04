package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import kr.ucc.organization.*;
import kr.ucc.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ClubDetailsTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  UserRepository users;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  Long leader, student, staff, org;

  @BeforeEach
  void setup() {
    leader = users.save(new User("clubleader@test.dev", "unused", "대표", null, null)).id;
    student = users.save(new User("clubstudent@test.dev", "unused", "신청자", null, null)).id;
    staff = users.save(new User("clubstaff@test.dev", "unused", "운영진", null, null)).id;
    var club = new Organization("합주 동아리", null, "함께 연주해요");
    club.type = Organization.Type.CLUB;
    org = organizations.save(club).id;
    memberships.save(new Membership(org, leader, Membership.Role.LEADER));
    memberships.save(new Membership(org, staff, Membership.Role.STAFF));
  }

  @Autowired
  ClubRecruitmentService recruitment;

  @Autowired
  kr.ucc.notification.NotificationRepository notifications;

  @Test
  void closedClubBlocksJoiningAndOpeningNotifiesOnce() throws Exception {
    String path = "/api/v1/organizations/" + org + "/club-details";
    mvc
      .perform(
        put(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            "{\"meetingCycle\":\"매주 목요일\",\"entryFee\":10000,\"recruitmentMode\":\"CLOSED\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("open").value(false));
    mvc
      .perform(
        post("/api/v1/organizations/" + org + "/membership-requests")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(
        put(path + "/subscription")
          .with(user(student.toString()))
          .with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("subscribed").value(true));
    mvc
      .perform(
        put(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"entryFee\":0,\"recruitmentMode\":\"ALWAYS\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("open").value(true));
    recruitment.tick();
    recruitment.tick();
    assertEquals(1, notifications.findTop100ByUserIdOrderByCreatedAtDesc(student).size());
    mvc
      .perform(
        post("/api/v1/organizations/" + org + "/membership-requests")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isOk());
  }

  @Test
  void clubDetailsValidatePermissionsAndPeriods() throws Exception {
    String path = "/api/v1/organizations/" + org + "/club-details";
    mvc
      .perform(
        put(path)
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"entryFee\":0,\"recruitmentMode\":\"ALWAYS\"}")
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        put(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"entryFee\":0,\"recruitmentMode\":\"PERIOD\"}")
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(
        put(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            "{\"entryFee\":0,\"recruitmentMode\":\"ALWAYS\",\"photoUrl\":\"javascript:alert(1)\"}"
          )
      )
      .andExpect(status().isBadRequest());
  }
}
