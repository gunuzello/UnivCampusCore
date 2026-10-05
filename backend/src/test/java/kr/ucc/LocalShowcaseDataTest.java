package kr.ucc;

import static org.junit.jupiter.api.Assertions.*;

import kr.ucc.common.LocalShowcaseData;
import kr.ucc.organization.*;
import kr.ucc.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LocalShowcaseDataTest {

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  PasswordEncoder passwords;

  @Autowired
  UserRepository users;

  @Autowired
  OrganizationRepository organizations;

  @Autowired
  MembershipRepository memberships;

  private LocalShowcaseData prepare() {
    var leader = users.save(
      new User("leader@ucc.local", "preserved-password", "기존 대표", "ITM전공", "26100001")
    );
    users.save(
      new User("student@ucc.local", "preserved-password", "기존 학생", "ITM전공", "26100002")
    );
    users.save(
      new User("staff@ucc.local", "preserved-password", "기존 운영진", "ITM전공", "26100003")
    );
    var org = organizations.save(new Organization("기존 학생회", "ITM전공", "기존 소개"));
    memberships.save(new Membership(org.id, leader.id, Membership.Role.LEADER));
    users.flush();
    organizations.flush();
    memberships.flush();
    return new LocalShowcaseData(jdbc, passwords);
  }

  @Test
  void repeatedStartupPreservesEditedAndDeletedFixturesAndExistingUsers() {
    var seed = prepare();
    var args = new DefaultApplicationArguments(new String[0]);
    seed.run(args);
    int markers = jdbc.queryForObject(
      "select count(*) from local_showcase_fixtures",
      Integer.class
    );
    Long program = jdbc.queryForObject(
      "select entity_id from local_showcase_fixtures where fixture_key='program.design'",
      Long.class
    );
    jdbc.update("update programs set title='사용자가 수정한 제목' where id=?", program);
    jdbc.update("delete from saved_activities where target_id=? and type='PROGRAM'", program);
    jdbc.update("delete from programs where id=?", program);
    seed.run(args);
    assertEquals(
      markers,
      jdbc.queryForObject("select count(*) from local_showcase_fixtures", Integer.class)
    );
    assertEquals(
      0,
      jdbc.queryForObject("select count(*) from programs where id=?", Integer.class, program)
    );
    assertEquals(
      "preserved-password",
      jdbc.queryForObject(
        "select password_hash from app_users where email='leader@ucc.local'",
        String.class
      )
    );
    assertEquals(
      "기존 대표",
      jdbc.queryForObject("select name from app_users where email='leader@ucc.local'", String.class)
    );
    Long web = jdbc.queryForObject(
      "select entity_id from local_showcase_fixtures where fixture_key='program.web'",
      Long.class
    );
    jdbc.update("update programs set title='수정 유지' where id=?", web);
    seed.run(args);
    assertEquals(
      "수정 유지",
      jdbc.queryForObject("select title from programs where id=?", String.class, web)
    );
  }

  @Test
  void fixtureApplicationsAndWorkspaceDataAreConsistent() {
    var seed = prepare();
    seed.run(new DefaultApplicationArguments(new String[0]));
    assertEquals(
      6,
      jdbc.queryForObject(
        "select count(*) from local_showcase_fixtures where entity_type='programs'",
        Integer.class
      )
    );
    assertEquals(
      4,
      jdbc.queryForObject(
        "select count(*) from local_showcase_fixtures where entity_type='teams'",
        Integer.class
      )
    );
    assertEquals(
      0,
      jdbc.queryForObject(
        "select count(*) from events where opens_at>=closes_at or closes_at>starts_at or starts_at>=ends_at",
        Integer.class
      )
    );
    assertEquals(
      0,
      jdbc.queryForObject(
        "select count(*) from teams t where 1+(select count(*) from team_applications a where a.team_id=t.id and a.status='ACCEPTED')>t.capacity",
        Integer.class
      )
    );
    assertEquals(
      0,
      jdbc.queryForObject(
        "select count(*) from event_applications a where (select count(*) from event_answers x where x.application_id=a.id)<>(select count(*) from event_questions q where q.event_id=a.event_id)",
        Integer.class
      )
    );
    assertEquals(
      0,
      jdbc.queryForObject(
        "select count(*) from team_entries e join teams t on t.id=e.team_id where e.assignee_id is not null and e.assignee_id<>t.owner_id and not exists(select 1 from team_applications a where a.team_id=t.id and a.user_id=e.assignee_id and a.status='ACCEPTED')",
        Integer.class
      )
    );
  }
}
