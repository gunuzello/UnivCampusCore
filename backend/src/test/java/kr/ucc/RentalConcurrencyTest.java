package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import kr.ucc.common.ApiException;
import kr.ucc.organization.*;
import kr.ucc.rental.*;
import kr.ucc.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RentalConcurrencyTest {

  @Autowired
  RentalService rentals;

  @Autowired
  UserRepository users;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  @Autowired
  JdbcTemplate jdbc;

  @Test
  void exactlyOneRequestReservesLastItem() throws Exception {
    String key = UUID.randomUUID().toString();
    var a = users.save(new User(key + "a@test.dev", "unused", "A", null, null));
    var b = users.save(new User(key + "b@test.dev", "unused", "B", null, null));
    var org = organizations.save(new Organization("대여 경합", null, null));
    memberships.save(new Membership(org.id, a.id, Membership.Role.LEADER));
    var item = rentals.create(org.id, a.id, new RentalService.Input("마지막 우산", "", 1, 7, true));
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var tasks = new ArrayList<Future<String>>();
      for (Long user : List.of(a.id, b.id))
        tasks.add(
          pool.submit(() -> {
            start.await();
            try {
              rentals.request(item.id(), user, new RentalService.Request(1));
              return "OK";
            } catch (ApiException e) {
              return e.code;
            }
          })
        );
      start.countDown();
      var results = new ArrayList<String>();
      for (var task : tasks) results.add(task.get(15, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(results, "OK"));
      assertEquals(1, Collections.frequency(results, "OUT_OF_STOCK"));
      assertEquals(0, rentals.list(org.id, a.id).get(0).availableQuantity());
    } finally {
      pool.shutdownNow();
      jdbc.update("delete from rental_loans where item_id=?", item.id());
      jdbc.update("delete from rental_items where id=?", item.id());
      jdbc.update("delete from notifications where user_id in (?,?)", a.id, b.id);
      jdbc.update("delete from memberships where organization_id=?", org.id);
      jdbc.update("delete from organizations where id=?", org.id);
      jdbc.update("delete from app_users where id in (?,?)", a.id, b.id);
    }
  }
}
