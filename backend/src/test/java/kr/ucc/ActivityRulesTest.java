package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import kr.ucc.common.ApiException;
import kr.ucc.event.*;
import kr.ucc.organization.*;
import kr.ucc.recruitment.*;
import kr.ucc.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityRulesTest {

  @Autowired
  EventService events;

  @Autowired
  RecruitmentService recruitments;

  @Autowired
  UserRepository users;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  Long leader, student, org;

  @BeforeEach
  void setup() {
    leader = users.save(new User("lead@rules.test", "unused", "대표", null, null)).id;
    student = users.save(new User("student@rules.test", "unused", "학생", null, null)).id;
    org = organizations.save(new Organization("학생회", null, null)).id;
    memberships.save(new Membership(org, leader, Membership.Role.LEADER));
  }

  EventService.Input input(int capacity, Instant open, Instant close) {
    return new EventService.Input(
      "간식행사",
      "안내",
      open,
      close,
      Instant.now().plusSeconds(7200),
      Instant.now().plusSeconds(10800),
      "학생회실",
      capacity,
      List.of("알레르기")
    );
  }

  @Test
  void duplicateCapacityCancellationAndAuthorization() {
    var d = input(1, Instant.now().minusSeconds(100), Instant.now().plusSeconds(3600));
    var e = events.create(org, leader, d);
    assertThrows(ApiException.class, () -> events.get(e.id(), student));
    events.status(e.id(), leader, Event.Status.PUBLISHED);
    var a = events.apply(e.id(), student, new EventService.Apply(List.of("없음")));
    assertEquals(EventApplication.Status.REGISTERED, a.status());
    assertEquals(
      "ALREADY_APPLIED",
      assertThrows(ApiException.class, () ->
        events.apply(e.id(), student, new EventService.Apply(List.of("없음")))
      ).code
    );
    assertEquals(
      "CAPACITY_FULL",
      assertThrows(ApiException.class, () ->
        events.apply(e.id(), leader, new EventService.Apply(List.of("없음")))
      ).code
    );
    assertThrows(ApiException.class, () -> events.update(e.id(), student, d));
    events.cancel(e.id(), student);
    assertEquals(
      EventApplication.Status.REGISTERED,
      events.apply(e.id(), leader, new EventService.Apply(List.of("없음"))).status()
    );
    assertThrows(ApiException.class, () -> events.status(e.id(), leader, Event.Status.COMPLETED));
  }

  @Test
  void periodsQuestionsAndCapacityChangesAreProtected() {
    var e = events.create(
      org,
      leader,
      input(2, Instant.now().plusSeconds(100), Instant.now().plusSeconds(3600))
    );
    events.status(e.id(), leader, Event.Status.PUBLISHED);
    assertEquals(
      "NOT_STARTED",
      assertThrows(ApiException.class, () ->
        events.apply(e.id(), student, new EventService.Apply(List.of("없음")))
      ).code
    );
    events.update(
      e.id(),
      leader,
      input(2, Instant.now().minusSeconds(100), Instant.now().plusSeconds(3600))
    );
    assertEquals(
      "INVALID_ANSWERS",
      assertThrows(ApiException.class, () ->
        events.apply(e.id(), student, new EventService.Apply(List.of()))
      ).code
    );
    events.apply(e.id(), student, new EventService.Apply(List.of("없음")));
    events.apply(e.id(), leader, new EventService.Apply(List.of("없음")));
    assertEquals(
      "CAPACITY_TOO_SMALL",
      assertThrows(ApiException.class, () ->
        events.update(
          e.id(),
          leader,
          input(1, Instant.now().minusSeconds(100), Instant.now().plusSeconds(3600))
        )
      ).code
    );
  }

  @Test
  void recruitmentQuestionsAndReview() {
    var d = new RecruitmentService.Input(
      "신입부원",
      "설명",
      Instant.now().minusSeconds(10),
      Instant.now().plusSeconds(3600),
      List.of("지원 동기", "관심 부서")
    );
    var r = recruitments.create(org, leader, d);
    recruitments.status(r.id(), leader, Recruitment.Status.PUBLISHED);
    var a = recruitments.apply(
      r.id(),
      student,
      new RecruitmentService.Apply(List.of("함께하고 싶어요", "기획"))
    );
    assertThrows(ApiException.class, () -> recruitments.applicants(r.id(), student));
    recruitments.result(
      r.id(),
      a.id(),
      leader,
      new RecruitmentService.ApplicationStatus(RecruitmentApplication.Status.ACCEPTED)
    );
    assertEquals(
      RecruitmentApplication.Status.ACCEPTED,
      recruitments.mine(r.id(), student).status()
    );
  }
}
