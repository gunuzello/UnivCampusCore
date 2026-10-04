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
class TeamRulesTest {

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

  Long team() throws Exception {
    var result = mvc
      .perform(
        post("/api/v1/teams")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            json.writeValueAsString(
              Map.of(
                "title",
                "공모전 팀",
                "content",
                "함께 만들어 봐요",
                "roles",
                "개발, 디자인",
                "tags",
                "개발",
                "capacity",
                2,
                "deadline",
                java.time.Instant.now().plusSeconds(86400).toString()
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("team").get("id").asLong();
  }

  void apply(Long team, Long userId) throws Exception {
    mvc
      .perform(
        post("/api/v1/teams/" + team + "/applications")
          .with(user(userId.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"role\":\"개발\",\"message\":\"함께 할게요\"}")
      )
      .andExpect(status().isOk());
  }

  @Test
  void teamPrivacyCapacityAndCancellation() throws Exception {
    Long id = team();
    apply(id, student);
    apply(id, staff);
    mvc
      .perform(get("/api/v1/teams/" + id + "/applications").with(user(student.toString())))
      .andExpect(status().isForbidden());
    mvc
      .perform(get("/api/v1/teams/" + id + "/entries").with(user(student.toString())))
      .andExpect(status().isForbidden());
    mvc
      .perform(
        patch("/api/v1/teams/" + id + "/applications/" + student + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("memberCount").value(2));
    mvc
      .perform(
        patch("/api/v1/teams/" + id + "/applications/" + staff + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(
        post("/api/v1/teams/" + id + "/leave")
          .with(user(student.toString()))
          .with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("member").value(false));
    apply(id, student);
  }

  @Test
  void teamWorkspaceAndCompletedRecord() throws Exception {
    Long id = team();
    var body = Map.of(
      "kind",
      "LINK",
      "title",
      "제안서",
      "content",
      "기록",
      "url",
      "https://example.com/doc",
      "done",
      false
    );
    mvc
      .perform(
        post("/api/v1/teams/" + id + "/entries")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        post("/api/v1/teams/" + id + "/entries")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        patch("/api/v1/teams/" + id + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"COMPLETED\"}")
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        post("/api/v1/teams/" + id + "/entries")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(get("/api/v1/teams/" + id + "/entries").with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].title").value("제안서"));
  }
}
