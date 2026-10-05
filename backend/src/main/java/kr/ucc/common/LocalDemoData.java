package kr.ucc.common;

import java.time.Instant;
import java.util.*;
import kr.ucc.archive.*;
import kr.ucc.event.*;
import kr.ucc.meeting.*;
import kr.ucc.organization.*;
import kr.ucc.recruitment.*;
import kr.ucc.rental.RentalService;
import kr.ucc.schedule.*;
import kr.ucc.user.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@Order(10)
public class LocalDemoData implements ApplicationRunner {

  private final UserRepository users;
  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final PasswordEncoder passwords;
  private final EventService events;
  private final RecruitmentService recruitments;
  private final MeetingService meetings;
  private final ScheduleRepository schedules;
  private final HandoverNoteRepository notes;
  private final ExternalLinkRepository links;
  private final RentalService rentals;

  public LocalDemoData(
    UserRepository users,
    OrganizationRepository organizations,
    MembershipRepository memberships,
    PasswordEncoder passwords,
    EventService events,
    RecruitmentService recruitments,
    MeetingService meetings,
    ScheduleRepository schedules,
    HandoverNoteRepository notes,
    ExternalLinkRepository links,
    RentalService rentals
  ) {
    this.users = users;
    this.organizations = organizations;
    this.memberships = memberships;
    this.passwords = passwords;
    this.events = events;
    this.recruitments = recruitments;
    this.meetings = meetings;
    this.schedules = schedules;
    this.notes = notes;
    this.links = links;
    this.rentals = rentals;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (users.findByEmail("leader@ucc.local").isPresent()) return;
    var leader = users.save(
      new User(
        "leader@ucc.local",
        passwords.encode("ucc-local-2026!"),
        "김민서",
        "ITM전공",
        "26100001"
      )
    );
    var student = users.save(
      new User(
        "student@ucc.local",
        passwords.encode("ucc-local-2026!"),
        "이서준",
        "ITM전공",
        "26100002"
      )
    );
    var staff = users.save(
      new User(
        "staff@ucc.local",
        passwords.encode("ucc-local-2026!"),
        "박지우",
        "ITM전공",
        "25100003"
      )
    );
    var org = organizations.save(
      new Organization(
        "ITM전공 제16대 학생회 원세컨드",
        "ITM전공",
        "계속해서 이어지는 캠퍼스의 이야기. 학생회가 함께 만들고 다음 기수로 이어가요."
      )
    );
    memberships.save(new Membership(org.id, leader.id, Membership.Role.LEADER));
    memberships.save(new Membership(org.id, staff.id, Membership.Role.STAFF));
    var now = Instant.now();
    var auth = new UsernamePasswordAuthenticationToken(leader.id.toString(), null, List.of());
    var event = events.create(
      org.id,
      leader.id,
      new EventService.Input(
        "중간고사 간식행사",
        "시험 준비로 바쁜 여러분을 위해 간식을 준비했어요. 신청 후 학생회실에서 받아 가세요.",
        now.minusSeconds(3600),
        now.plusSeconds(86400 * 3),
        now.plusSeconds(86400 * 4),
        now.plusSeconds(86400 * 4 + 7200),
        "학생회실",
        60,
        List.of("식품 알레르기가 있나요?")
      )
    );
    events.status(event.id(), leader.id, Event.Status.PUBLISHED);
    events.apply(event.id(), student.id, new EventService.Apply(List.of("없어요")));
    var past = events.create(
      org.id,
      leader.id,
      new EventService.Input(
        "지난 학기 간식행사",
        "지난 학기 운영 기록이에요. 다음 행사에서 복제해서 사용할 수 있어요.",
        now.minusSeconds(86400 * 35),
        now.minusSeconds(86400 * 32),
        now.minusSeconds(86400 * 30),
        now.minusSeconds(86400 * 30 - 7200),
        "학생회실",
        60,
        List.of("식품 알레르기가 있나요?")
      )
    );
    events.status(past.id(), leader.id, Event.Status.PUBLISHED);
    events.status(past.id(), leader.id, Event.Status.CLOSED);
    events.status(past.id(), leader.id, Event.Status.COMPLETED);
    var recruitment = recruitments.create(
      org.id,
      leader.id,
      new RecruitmentService.Input(
        "제16대 학생회 신입부원 모집",
        "기획, 홍보, 사무, 집행 부서에서 함께할 신입부원을 모집해요.",
        now.minusSeconds(3600),
        now.plusSeconds(86400 * 10),
        List.of("지원 동기를 알려 주세요.", "관심 있는 부서는 어디인가요?")
      )
    );
    recruitments.status(recruitment.id(), leader.id, Recruitment.Status.PUBLISHED);
    recruitments.apply(
      recruitment.id(),
      student.id,
      new RecruitmentService.Apply(List.of("학과 행사를 함께 만들고 싶어요.", "기획부"))
    );
    meetings.create(
      org.id,
      leader.id,
      new MeetingService.Input(
        "10월 정기회의",
        now.plusSeconds(86400),
        now.plusSeconds(86400 + 3600),
        "간식행사 운영 역할을 나누고 모집 진행 상황을 확인해요.",
        Set.of(leader.id, staff.id),
        List.of("간식행사 준비 상황", "신입부원 모집 안내"),
        List.of()
      )
    );
    meetings.create(
      org.id,
      leader.id,
      new MeetingService.Input(
        "지난 정기회의",
        now.minusSeconds(86400 * 7),
        now.minusSeconds(86400 * 7 - 3600),
        "지난 회의의 기록입니다.",
        Set.of(leader.id, staff.id),
        List.of("간식행사 일정"),
        List.of("정원은 60명으로 운영한다.")
      )
    );
    schedules.save(
      new Schedule(
        org.id,
        "간식 수령 및 준비",
        now.plusSeconds(86400 * 2),
        now.plusSeconds(86400 * 2 + 3600),
        "운영진이 학생회실에서 준비해요."
      )
    );
    rentals.create(
      org.id,
      leader.id,
      new RentalService.Input("우산", "비 오는 날 빌려 쓰는 학생회 우산", 5, 7, true)
    );
    rentals.create(
      org.id,
      leader.id,
      new RentalService.Input("C타입 충전기", "학생회실에서 수령하고 반납해 주세요.", 3, 3, true)
    );
    var note = notes.save(
      new HandoverNote(
        org.id,
        "다음 기수에 전하는 행사 운영 팁",
        "2026년 · 제16대",
        "신청 정원과 수령 시간을 미리 확인하세요. 지난 행사와 회의 기록을 함께 참고하면 준비 시간을 줄일 수 있어요."
      )
    );
    links.save(
      new ExternalLink(
        org.id,
        "NOTE",
        note.id,
        "외부 자료 링크 예시",
        "https://example.com",
        "실제 기획서·결과 보고서 주소로 바꿔 주세요."
      )
    );
  }
}
