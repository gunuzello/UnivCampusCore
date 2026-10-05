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
class OrganizationWorkTest {

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
  void roleRequestsRequireRepresentativeApproval() throws Exception {
    memberships.save(new Membership(org, student, Membership.Role.MEMBER));
    String path = "/api/v1/organizations/" + org + "/work";
    String body =
      "{\"kind\":\"ROLE\",\"title\":\"운영진 신청\",\"content\":\"도와드릴게요\",\"requestedRole\":\"STAFF\"}";
    var r = mvc
      .perform(
        post(path)
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body)
      )
      .andExpect(status().isOk())
      .andReturn();
    Long id = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    mvc
      .perform(
        post(path)
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body)
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(
        patch(path + "/" + id + "/status")
          .with(user(staff.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        patch(path + "/" + id + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isOk());
    assertEquals(
      Membership.Role.STAFF,
      memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role
    );
  }

  private Long createStaffRequest() throws Exception {
    memberships.save(new Membership(org, student, Membership.Role.MEMBER));
    var result = mvc
      .perform(
        post("/api/v1/organizations/" + org + "/work")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            json.writeValueAsString(
              Map.of(
                "kind",
                "ROLE",
                "title",
                "운영진 신청",
                "content",
                "모집 활동을 돕고 싶어요.",
                "requestedRole",
                "STAFF"
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  void staleRoleRequestCannotDemoteTheLastRepresentative() throws Exception {
    Long id = createStaffRequest();
    memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role =
      Membership.Role.LEADER;
    memberships.findByOrganizationIdAndUserId(org, leader).orElseThrow().role =
      Membership.Role.STAFF;
    String path = "/api/v1/organizations/" + org + "/work";
    mvc
      .perform(
        patch(path + "/" + id + "/status")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("LAST_LEADER"));
    assertEquals(
      Membership.Role.LEADER,
      memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role
    );
    assertEquals(1, memberships.countByOrganizationIdAndRole(org, Membership.Role.LEADER));
    mvc
      .perform(get(path).with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  void requestCannotBeApprovedAfterTheRequestedRoleWasGrantedElsewhere() throws Exception {
    Long id = createStaffRequest();
    memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role =
      Membership.Role.STAFF;
    mvc
      .perform(
        patch("/api/v1/organizations/" + org + "/work/" + id + "/status")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("ROLE_CHANGED"));
    assertEquals(
      Membership.Role.STAFF,
      memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role
    );
  }

  @Test
  void processingAnApprovedRequestAgainDoesNotOverwriteTheCurrentRole() throws Exception {
    Long id = createStaffRequest();
    String path = "/api/v1/organizations/" + org + "/work/" + id + "/status";
    mvc
      .perform(
        patch(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isOk());
    memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role =
      Membership.Role.LEADER;
    mvc
      .perform(
        patch(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ACCEPTED\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    assertEquals(
      Membership.Role.LEADER,
      memberships.findByOrganizationIdAndUserId(org, student).orElseThrow().role
    );
  }

  @Test
  void suggestionPrivacyAndTaskAssignment() throws Exception {
    String path = "/api/v1/organizations/" + org + "/work";
    var r = mvc
      .perform(
        post(path)
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"kind\":\"SUGGESTION\",\"title\":\"건의\",\"content\":\"비공개 의견\"}")
      )
      .andExpect(status().isOk())
      .andReturn();
    Long id = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    Long other = users.save(new User("work-other@test.dev", "unused", "다른 학생", null, null)).id;
    mvc
      .perform(get(path).with(user(other.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
    mvc
      .perform(
        patch(path + "/" + id + "/status")
          .with(user(staff.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"ANSWERED\",\"response\":\"확인했어요\"}")
      )
      .andExpect(status().isOk());
    memberships.save(new Membership(org, student, Membership.Role.MEMBER));
    r = mvc
      .perform(
        post(path)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            json.writeValueAsString(
              Map.of(
                "kind",
                "TASK",
                "title",
                "자료 작성",
                "content",
                "결과 문서 작성",
                "assigneeId",
                student
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    id = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    mvc
      .perform(
        patch(path + "/" + id + "/status")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"DONE\"}")
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        patch(path + "/" + id)
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"kind\":\"TASK\",\"title\":\"변경\",\"content\":\"변경\"}")
      )
      .andExpect(status().isForbidden());
  }
}
