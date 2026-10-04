package kr.ucc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import kr.ucc.event.*;
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
class OperationsTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  UserRepository users;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  @Autowired
  ObjectMapper json;

  @Autowired
  EventService events;

  Long leader, student, org;

  @BeforeEach
  void setup() {
    leader = users.save(new User("opsleader@test.dev", "unused", "대표", null, null)).id;
    student = users.save(new User("opsstudent@test.dev", "unused", "학생", null, null)).id;
    org = organizations.save(new Organization("학생회", null, null)).id;
    memberships.save(new Membership(org, leader, Membership.Role.LEADER));
  }

  String body(Object value) throws Exception {
    return json.writeValueAsString(value);
  }

  @Test
  void meetingCalendarAndArchiveProtectInternalRecords() throws Exception {
    var now = Instant.now();
    var d = Map.of(
      "title",
      "정기회의",
      "startsAt",
      now.minusSeconds(7200),
      "endsAt",
      now.minusSeconds(3600),
      "content",
      "행사 운영 계획",
      "attendees",
      List.of(leader),
      "agendas",
      List.of("간식행사"),
      "decisions",
      List.of("다음 주 진행")
    );
    mvc
      .perform(
        post("/api/v1/organizations/" + org + "/meetings")
          .with(user(student.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(d))
      )
      .andExpect(status().isForbidden());
    var result = mvc
      .perform(
        post("/api/v1/organizations/" + org + "/meetings")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(d))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("decisions[0]").value("다음 주 진행"))
      .andReturn();
    long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    mvc
      .perform(get("/api/v1/meetings/" + id).with(user(student.toString())))
      .andExpect(status().isForbidden());
    String path = "/api/v1/organizations/" + org + "/calendar";
    mvc
      .perform(
        get(path)
          .param("from", now.minusSeconds(86400).toString())
          .param("to", now.plusSeconds(86400).toString())
          .with(user(student.toString()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
    mvc
      .perform(
        get(path)
          .param("from", now.minusSeconds(86400).toString())
          .param("to", now.plusSeconds(86400).toString())
          .with(user(leader.toString()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].type").value("MEETING"));
    mvc
      .perform(get("/api/v1/organizations/" + org + "/archive").with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].title").value("정기회의"));
  }

  @Test
  void pastSchedulesHavePrivateDetailsAndArchiveLinks() throws Exception {
    var now = Instant.now();
    var result = mvc
      .perform(
        post("/api/v1/organizations/" + org + "/schedules")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            body(
              Map.of(
                "title",
                "지난 운영 일정",
                "startsAt",
                now.minusSeconds(7200),
                "endsAt",
                now.minusSeconds(3600),
                "description",
                "운영 기록"
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    String id = json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    mvc
      .perform(get("/api/v1/schedules/" + id).with(user(student.toString())))
      .andExpect(status().isForbidden());
    mvc
      .perform(get("/api/v1/schedules/" + id).with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("schedule.path").value("/schedules/" + id));
    mvc
      .perform(get("/api/v1/organizations/" + org + "/archive").with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].type").value("SCHEDULE"));
    mvc
      .perform(
        post("/api/v1/organizations/" + org + "/links")
          .param("type", "SCHEDULE")
          .param("targetId", id)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(Map.of("title", "자료", "url", "https://example.com", "description", "")))
      )
      .andExpect(status().isOk());
  }

  @Test
  void notesAndLinksRejectUnsafeUrlsAndCrossOrganizationTargets() throws Exception {
    var result = mvc
      .perform(
        post("/api/v1/organizations/" + org + "/handover-notes")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(
            body(
              Map.of(
                "title",
                "인수인계",
                "period",
                "2026",
                "content",
                "간식행사는 정원을 먼저 정해요."
              )
            )
          )
      )
      .andExpect(status().isOk())
      .andReturn();
    String id = json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    String path = "/api/v1/organizations/" + org + "/links";
    mvc
      .perform(
        post(path)
          .param("type", "NOTE")
          .param("targetId", id)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(Map.of("title", "자료", "url", "javascript:alert(1)")))
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(
        post(path)
          .param("type", "NOTE")
          .param("targetId", id)
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(Map.of("title", "기획서", "url", "https://example.com/document")))
      )
      .andExpect(status().isOk());
    mvc
      .perform(get(path).param("type", "NOTE").param("targetId", id).with(user(student.toString())))
      .andExpect(status().isForbidden());
    var other = organizations.save(new Organization("다른 학생회", null, null));
    memberships.save(new Membership(other.id, leader, Membership.Role.LEADER));
    mvc
      .perform(
        get("/api/v1/organizations/" + other.id + "/links")
          .param("type", "NOTE")
          .param("targetId", id)
          .with(user(leader.toString()))
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void completedEventCanBeArchivedAndCopiedWithoutParticipants() throws Exception {
    var now = Instant.now();
    var e = events.create(
      org,
      leader,
      new EventService.Input(
        "지난 행사",
        null,
        now.minusSeconds(20000),
        now.minusSeconds(15000),
        now.minusSeconds(10000),
        now.minusSeconds(5000),
        "학생회실",
        30,
        List.of("질문")
      )
    );
    events.status(e.id(), leader, Event.Status.PUBLISHED);
    events.status(e.id(), leader, Event.Status.CLOSED);
    events.status(e.id(), leader, Event.Status.COMPLETED);
    mvc
      .perform(get("/api/v1/organizations/" + org + "/archive").with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].type").value("EVENT"));
    var d = new EventService.Input(
      "새 행사",
      null,
      now,
      now.plusSeconds(3600),
      now.plusSeconds(7200),
      now.plusSeconds(10800),
      "학생회실",
      30,
      List.of("질문")
    );
    mvc
      .perform(
        post("/api/v1/events/" + e.id() + "/copies")
          .with(user(leader.toString()))
          .with(csrf())
          .contentType("application/json")
          .content(body(d))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("applicationCount").value(0))
      .andExpect(jsonPath("status").value("DRAFT"));
  }

  @Test
  void noApplicationReturnsNoContentAndMyApplicationsArePrivate() throws Exception {
    var now = Instant.now();
    var e = events.create(
      org,
      leader,
      new EventService.Input(
        "신청 조회",
        null,
        now.minusSeconds(10),
        now.plusSeconds(3600),
        now.plusSeconds(7200),
        now.plusSeconds(10800),
        "학생회실",
        30,
        List.of()
      )
    );
    events.status(e.id(), leader, Event.Status.PUBLISHED);
    mvc
      .perform(get("/api/v1/events/" + e.id() + "/applications/me").with(user(student.toString())))
      .andExpect(status().isNoContent());
    events.apply(e.id(), student, new EventService.Apply(List.of()));
    mvc
      .perform(get("/api/v1/me/applications").with(user(student.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].title").value("신청 조회"));
    mvc
      .perform(get("/api/v1/me/applications").with(user(leader.toString())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }
}
