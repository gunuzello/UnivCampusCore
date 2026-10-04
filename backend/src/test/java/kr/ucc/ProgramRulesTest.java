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
class ProgramRulesTest {

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

  @Test
  void programPublicationAndPermissions() throws Exception {
    var now = java.time.Instant.now();
    var body = new java.util.HashMap<String, Object>();
    body.put("title", "지역 공모전");
    body.put("content", "참여 안내");
    body.put("category", "공모전");
    body.put("tags", "디자인");
    body.put("applicationUrl", "https://example.com/apply");
    body.put("deadline", now.plusSeconds(3600).toString());
    body.put("startsAt", now.plusSeconds(7200).toString());
    body.put("endsAt", now.plusSeconds(10800).toString());
    body.put("published", false);
    String path = "/api/v1/organizations/" + org + "/programs";
    mvc
      .perform(
        post(path)
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isForbidden());
    var result = mvc
      .perform(
        post(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isOk())
      .andReturn();
    Long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc
      .perform(get("/api/v1/programs/" + id).with(user(student.toString())))
      .andExpect(status().isNotFound());
    body.put("published", true);
    mvc
      .perform(
        patch("/api/v1/programs/" + id)
          .with(user(staff.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        get("/api/v1/programs?search=지역&category=공모전&openOnly=true").with(
          user(student.toString())
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(id));
    body.put("applicationUrl", "javascript:alert(1)");
    mvc
      .perform(
        patch("/api/v1/programs/" + id)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isBadRequest());
  }
}
