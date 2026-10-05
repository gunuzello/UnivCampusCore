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
class PersonalRulesTest {

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
  kr.ucc.team.TeamRepository teams;

  @Test
  void discoveryFallsBackToOpenActivitiesAndUsesSkillsWithoutLeakingClosedTeams() throws Exception {
    for (String state : new String[] { "OPEN", "COMPLETED", "EXPIRED" }) {
      var team = new kr.ucc.team.Team();
      team.ownerId = leader;
      team.title = "React 프로젝트 " + state;
      team.content = "함께 개발해요";
      team.roles = "개발";
      team.tags = "React";
      team.capacity = 3;
      team.status = state.equals("EXPIRED") ? "OPEN" : state;
      team.deadline = java.time.Instant.now().plusSeconds(state.equals("EXPIRED") ? -60 : 86400);
      teams.save(team);
    }
    mvc
      .perform(get("/api/v1/me/recommendations").with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].score").value(0));
    mvc
      .perform(
        put("/api/v1/me/interests")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"skills\":\"react\"}")
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/me/recommendations").with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].score").value(1));
    mvc
      .perform(get("/api/v1/me/recommendations").with(user(staff.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].score").value(0));
  }

  @Test
  void profileAndSavedActivitiesArePrivateAndIdempotent() throws Exception {
    mvc
      .perform(
        put("/api/v1/me/interests")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            "{\"interests\":\"디자인\",\"activities\":\"공모전\",\"courses\":\"UX\",\"skills\":\"Figma\",\"portfolio\":\"내 경험\"}"
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/me/interests").with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("interests").value(""));
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
                "디자인 공모전",
                "content",
                "공모전에 참여해요",
                "roles",
                "디자인",
                "capacity",
                3,
                "deadline",
                java.time.Instant.now().plusSeconds(86400).toString()
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    Long id = json
      .readTree(result.getResponse().getContentAsString())
      .get("team")
      .get("id")
      .asLong();
    String save = "/api/v1/me/saved/TEAM/" + id;
    for (int i = 0; i < 2; i++) mvc
      .perform(put(save).with(user(student.toString())).with(csrf()))
      .andExpect(status().isOk());
    mvc
      .perform(get("/api/v1/me/saved").with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1));
    mvc
      .perform(get("/api/v1/me/saved").with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
    mvc
      .perform(get("/api/v1/me/recommendations").with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].score").value(2));
    mvc
      .perform(delete(save).with(user(student.toString())).with(csrf()))
      .andExpect(status().isNoContent());
    mvc
      .perform(get("/api/v1/me/saved").with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void privateTeamMeetingsDoNotAppearForOutsiders() throws Exception {
    var now = java.time.Instant.now();
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
                "팀 일정",
                "content",
                "설명",
                "roles",
                "디자인",
                "capacity",
                3,
                "deadline",
                now.plusSeconds(86400).toString()
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    Long id = json
      .readTree(result.getResponse().getContentAsString())
      .get("team")
      .get("id")
      .asLong();
    mvc
      .perform(
        post("/api/v1/teams/" + id + "/entries")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            json.writeValueAsString(
              Map.of(
                "kind",
                "MEETING",
                "title",
                "팀 내부 모임",
                "startsAt",
                now.plusSeconds(3600).toString(),
                "endsAt",
                now.plusSeconds(7200).toString(),
                "done",
                false
              )
            )
          )
      )
      .andExpect(status().isOk());
    String path =
      "/api/v1/me/calendar?from=" + now.toString() + "&to=" + now.plusSeconds(86400).toString();
    mvc
      .perform(get(path).with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
    mvc
      .perform(get(path).with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].type").value("TEAM"));
  }
}
