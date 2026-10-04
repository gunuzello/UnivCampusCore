package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import kr.ucc.common.ApiException;
import kr.ucc.event.*;
import kr.ucc.organization.*;
import kr.ucc.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EventConcurrencyTest {

  @Autowired
  EventService events;

  @Autowired
  UserRepository users;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  @Autowired
  JdbcTemplate jdbc;

  @Test
  void exactlyOneApplicantGetsLastSeat() throws Exception {
    String key = UUID.randomUUID().toString();
    var u1 = users.save(new User(key + "a@test.dev", "unused", "A", null, null));
    var u2 = users.save(new User(key + "b@test.dev", "unused", "B", null, null));
    var o = organizations.save(new Organization("정원 경합 테스트", null, null));
    memberships.save(new Membership(o.id, u1.id, Membership.Role.LEADER));
    var now = Instant.now();
    var e = events.create(
      o.id,
      u1.id,
      new EventService.Input(
        "마지막 자리",
        null,
        now.minusSeconds(10),
        now.plusSeconds(3600),
        now.plusSeconds(7200),
        now.plusSeconds(10800),
        "학생회실",
        1,
        List.of()
      )
    );
    events.status(e.id(), u1.id, Event.Status.PUBLISHED);
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      List<Future<String>> tasks = new ArrayList<>();
      for (Long id : List.of(u1.id, u2.id))
        tasks.add(
          pool.submit(() -> {
            start.await();
            try {
              events.apply(e.id(), id, new EventService.Apply(List.of()));
              return "OK";
            } catch (ApiException ex) {
              return ex.code;
            }
          })
        );
      start.countDown();
      var results = new ArrayList<String>();
      for (var task : tasks) results.add(task.get(15, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(results, "OK"));
      assertEquals(1, Collections.frequency(results, "CAPACITY_FULL"));
      assertEquals(1, events.get(e.id(), u1.id).applicationCount());
    } finally {
      pool.shutdownNow();
      jdbc.update("delete from notifications where user_id in (?,?)", u1.id, u2.id);
      jdbc.update(
        "delete from event_answers where application_id in(select id from event_applications where event_id=?)",
        e.id()
      );
      jdbc.update("delete from event_applications where event_id=?", e.id());
      jdbc.update("delete from event_questions where event_id=?", e.id());
      jdbc.update("delete from events where id=?", e.id());
      jdbc.update("delete from memberships where organization_id=?", o.id);
      jdbc.update("delete from organizations where id=?", o.id);
      jdbc.update("delete from app_users where id in (?,?)", u1.id, u2.id);
    }
  }
}
