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
class ClubMembershipTest {

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

  String path() {
    return "/api/v1/organizations/" + org + "/membership-requests";
  }

  Long apply() throws Exception {
    var result = mvc
      .perform(
        post(path())
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"message\":\"함께 합주하고 싶어요\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("status").value("PENDING"))
      .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  void noticesRespectPublicAndMemberVisibility() throws Exception {
    String path = "/api/v1/organizations/" + org + "/notices";
    mvc
      .perform(
        post(path)
          .with(user(staff.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            "{\"title\":\"합주 안내\",\"content\":\"구성원만 보는 장소 안내\",\"visibility\":\"MEMBERS\"}"
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get(path).with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
    mvc
      .perform(get(path).with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1));
    var result = mvc
      .perform(
        post(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            "{\"title\":\"동아리 소개\",\"content\":\"누구나 가입 신청할 수 있어요\",\"visibility\":\"PUBLIC\"}"
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    Long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc
      .perform(get(path).with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1));
    mvc
      .perform(
        patch(path + "/" + id)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            "{\"title\":\"동아리 소개\",\"content\":\"내부 안내로 전환\",\"visibility\":\"MEMBERS\"}"
          )
      )
      .andExpect(status().isOk());
    mvc
      .perform(get(path).with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void noticeWritesRejectStudentsAndOtherOrganizations() throws Exception {
    String path = "/api/v1/organizations/" + org + "/notices";
    String input = "{\"title\":\"안내\",\"content\":\"공지 내용\",\"visibility\":\"MEMBERS\"}";
    mvc
      .perform(
        post(path)
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(input)
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        post(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isBadRequest());
    var result = mvc
      .perform(
        post(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(input)
      )
      .andExpect(status().isOk())
      .andReturn();
    Long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    var other = organizations.save(new Organization("다른 동아리", null, null));
    memberships.save(new Membership(other.id, leader, Membership.Role.LEADER));
    mvc
      .perform(
        patch("/api/v1/organizations/" + other.id + "/notices/" + id)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(input)
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void createClubAndPreserveExistingStudentCouncilDefault() throws Exception {
    mvc
      .perform(
        post("/api/v1/organizations")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(json.writeValueAsString(Map.of("name", "새 동아리", "type", "CLUB")))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("type").value("CLUB"))
      .andExpect(jsonPath("role").value("LEADER"));
    mvc
      .perform(
        post("/api/v1/organizations")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"name\":\"새 학생회\"}")
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("type").value("STUDENT_COUNCIL"));
  }

  @Test
  void joiningRequiresLeaderApprovalAndCreatesOnlyMember() throws Exception {
    mvc
      .perform(get(path() + "/me").with(user(student.toString())))
      .andExpect(status().isNoContent());
    Long id = apply();
    mvc
      .perform(
        post(path())
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("code").value("ALREADY_REQUESTED"));
    mvc.perform(get(path()).with(user(student.toString()))).andExpect(status().isForbidden());
    mvc.perform(get(path()).with(user(staff.toString()))).andExpect(status().isForbidden());
    mvc
      .perform(
        patch(path() + "/" + id + "/status")
          .with(user(staff.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        patch(path() + "/" + id + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("status").value("ACCEPTED"));
    assertEquals(
      Membership.Role.MEMBER,
      memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role
    );
    mvc
      .perform(get("/api/v1/organizations/" + org + "/members").with(user(student.toString())))
      .andExpect(status().isOk());
    mvc
      .perform(
        post(path())
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("code").value("ALREADY_MEMBER"));
    mvc
      .perform(get("/api/v1/notifications").with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].path").value("/organization?org=" + org));
  }

  @Test
  void cancellationReapplyAndCrossOrganizationBoundaries() throws Exception {
    Long id = apply();
    mvc
      .perform(
        post(path() + "/" + id + "/cancellation")
          .with(user(staff.toString()))
          .with(csrf())
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        post(path() + "/" + id + "/cancellation")
          .with(user(student.toString()))
          .with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("status").value("CANCELLED"));
    mvc
      .perform(
        patch(path() + "/" + id + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isBadRequest());
    Long next = apply();
    assertNotEquals(id, next);
    var other = organizations.save(new Organization("다른 소속", null, null));
    memberships.save(new Membership(other.id, leader, Membership.Role.LEADER));
    mvc
      .perform(
        patch("/api/v1/organizations/" + other.id + "/membership-requests/" + next + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isNotFound());
    mvc
      .perform(
        patch(path() + "/" + next + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"REJECTED\"}")
      )
      .andExpect(status().isOk());
    assertTrue(memberships.findByOrganizationIdAndUserId(org, student).isEmpty());
  }
}
