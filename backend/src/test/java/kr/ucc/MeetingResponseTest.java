package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import kr.ucc.meeting.*;
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
class MeetingResponseTest {

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

  @Autowired
  MeetingResponseRepository responses;

  @Autowired
  MeetingRepository meetings;

  Long leader, member, outsider, org, meeting;
  Instant start;

  @BeforeEach
  void setup() throws Exception {
    leader = users.save(new User("rsvp-leader@test.dev", "unused", "대표", null, null)).id;
    member = users.save(new User("rsvp-member@test.dev", "unused", "구성원", null, null)).id;
    outsider = users.save(new User("rsvp-outsider@test.dev", "unused", "외부인", null, null)).id;
    org = organizations.save(new Organization("회의 소속", null, null)).id;
    memberships.save(new Membership(org, leader, Membership.Role.LEADER));
    memberships.save(new Membership(org, member, Membership.Role.MEMBER));
    start = Instant.now().plusSeconds(86400);
    var result = mvc
      .perform(
        post("/api/v1/organizations/" + org + "/meetings")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(start, Set.of(member)))
      )
      .andExpect(status().isOk())
      .andReturn();
    meeting = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  String body(Instant time, Set<Long> attendees) throws Exception {
    return json.writeValueAsString(
      Map.of(
        "title",
        "참석 의사 검증",
        "startsAt",
        time.toString(),
        "endsAt",
        time.plusSeconds(3600).toString(),
        "content",
        "내용",
        "attendees",
        attendees,
        "agendas",
        List.of(),
        "decisions",
        List.of()
      )
    );
  }

  String path() {
    return "/api/v1/meetings/" + meeting + "/responses";
  }

  void respond(Long userId, String status) throws Exception {
    mvc
      .perform(
        put(path() + "/me")
          .with(user(userId.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"" + status + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("status").value(status));
  }

  @Test
  void responseUpsertAndPrivacy() throws Exception {
    respond(member, "GOING");
    respond(member, "NOT_GOING");
    assertEquals(1, responses.findByMeetingId(meeting).size());
    mvc
      .perform(get(path()).with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].status").value("NOT_GOING"));
    mvc.perform(get(path()).with(user(outsider.toString()))).andExpect(status().isForbidden());
    mvc
      .perform(
        put(path() + "/me")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"GOING\"}")
      )
      .andExpect(status().isForbidden());
    mvc
      .perform(
        put(path() + "/me")
          .with(user(member.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{}")
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void scheduleChangesResetResponsesAndRemovedAttendeeCannotRespond() throws Exception {
    respond(member, "GOING");
    mvc
      .perform(
        patch("/api/v1/meetings/" + meeting)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(start.plusSeconds(3600), Set.of(member)))
      )
      .andExpect(status().isOk());
    assertTrue(responses.findByMeetingId(meeting).isEmpty());
    respond(member, "GOING");
    mvc
      .perform(
        patch("/api/v1/meetings/" + meeting)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(start.plusSeconds(3600), Set.of()))
      )
      .andExpect(status().isOk());
    assertTrue(responses.findByMeetingId(meeting).isEmpty());
    mvc
      .perform(
        put(path() + "/me")
          .with(user(member.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"GOING\"}")
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void responseClosesAtMeetingStart() throws Exception {
    var m = meetings.findById(meeting).orElseThrow();
    m.startsAt = Instant.now().minusSeconds(1);
    meetings.flush();
    mvc
      .perform(
        put(path() + "/me")
          .with(user(member.toString()))
          .with(csrf())
          .contentType("application/json")
          .content("{\"status\":\"GOING\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("code").value("RESPONSE_CLOSED"));
  }
}
