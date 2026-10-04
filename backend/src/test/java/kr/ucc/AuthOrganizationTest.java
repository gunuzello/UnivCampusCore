package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import kr.ucc.organization.*;
import kr.ucc.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthOrganizationTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  UserRepository users;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  @Test
  void signupHashesPasswordAndDuplicateEmailIsRejected() throws Exception {
    String body = "{\"email\":\"test@ucc.test\",\"password\":\"password123\",\"name\":\"학생\"}";
    mvc
      .perform(
        post("/api/v1/auth/signup").with(csrf()).contentType("application/json").content(body)
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("passwordHash").doesNotExist());
    assertTrue(
      encoder.matches("password123", users.findByEmail("test@ucc.test").orElseThrow().passwordHash)
    );
    mvc
      .perform(
        post("/api/v1/auth/signup").with(csrf()).contentType("application/json").content(body)
      )
      .andExpect(status().isConflict());
  }

  @Test
  void loginPersistsSessionAndLogoutProtectsProfile() throws Exception {
    users.save(new User("login@ucc.test", encoder.encode("password123"), "사용자", null, null));
    mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    var result = mvc
      .perform(
        post("/api/v1/auth/login")
          .with(csrf())
          .contentType("application/json")
          .content("{\"email\":\"login@ucc.test\",\"password\":\"password123\"}")
      )
      .andExpect(status().isOk())
      .andReturn();
    var session = (MockHttpSession) result.getRequest().getSession();
    mvc
      .perform(get("/api/v1/me").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("name").value("사용자"));
    mvc
      .perform(post("/api/v1/auth/logout").session(session).with(csrf()))
      .andExpect(status().isNoContent());
    assertTrue(session.isInvalid());
  }

  @Test
  void csrfIsRequiredAndLastLeaderCannotBeRemoved() throws Exception {
    var u = users.save(new User("leader@ucc.test", "unused", "대표", null, null));
    mvc
      .perform(
        post("/api/v1/organizations")
          .with(user(u.id.toString()))
          .contentType("application/json")
          .content("{\"name\":\"학생회\"}")
      )
      .andExpect(status().isForbidden());
    var o = organizations.save(new Organization("학생회", null, null));
    var m = memberships.save(new Membership(o.id, u.id, Membership.Role.LEADER));
    mvc
      .perform(
        delete("/api/v1/organizations/" + o.id + "/members/" + m.id)
          .with(user(u.id.toString()))
          .with(csrf())
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("code").value("LAST_LEADER"));
    mvc
      .perform(get("/api/v1/organizations/" + o.id + "/members").with(user("999999")))
      .andExpect(status().isForbidden());
  }
}
