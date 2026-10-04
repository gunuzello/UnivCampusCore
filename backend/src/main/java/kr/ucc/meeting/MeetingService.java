package kr.ucc.meeting;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import kr.ucc.common.ApiException;
import kr.ucc.organization.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MeetingService {

  private final MeetingRepository meetings;
  private final OrganizationAccess access;
  private final MembershipRepository memberships;

  public MeetingService(
    MeetingRepository meetings,
    OrganizationAccess access,
    MembershipRepository memberships
  ) {
    this.meetings = meetings;
    this.access = access;
    this.memberships = memberships;
  }

  public record Input(
    @NotBlank @Size(max = 200) String title,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @Size(max = 30000) String content,
    @NotNull @Size(max = 300) Set<Long> attendees,
    @NotNull @Size(max = 50) List<@NotBlank @Size(max = 3000) String> agendas,
    @NotNull @Size(max = 50) List<@NotBlank @Size(max = 3000) String> decisions
  ) {}

  public record View(
    Long id,
    Long organizationId,
    String title,
    Instant startsAt,
    Instant endsAt,
    String content,
    Set<Long> attendees,
    List<String> agendas,
    List<String> decisions,
    boolean canManage
  ) {}

  private View view(Meeting m, Long user) {
    return new View(
      m.id,
      m.organizationId,
      m.title,
      m.startsAt,
      m.endsAt,
      m.content,
      Set.copyOf(m.attendees),
      List.copyOf(m.agendas),
      List.copyOf(m.decisions),
      access.member(m.organizationId, user).role != Membership.Role.MEMBER
    );
  }

  private void set(Meeting m, Input d) {
    if (!d.startsAt().isBefore(d.endsAt())) throw ApiException.bad(
      "INVALID_PERIOD",
      "회의 종료는 시작 이후여야 합니다."
    );
    for (Long id : d.attendees())
      if (
        id == null || memberships.findByOrganizationIdAndUserId(m.organizationId, id).isEmpty()
      ) throw ApiException.bad("INVALID_ATTENDEE", "참석 대상은 조직 구성원이어야 합니다.");
    m.title = d.title();
    m.startsAt = d.startsAt();
    m.endsAt = d.endsAt();
    m.content = d.content();
    m.attendees = new HashSet<>(d.attendees());
    m.agendas = new ArrayList<>(d.agendas());
    m.decisions = new ArrayList<>(d.decisions());
  }

  @Transactional(readOnly = true)
  public List<View> list(Long org, Long user) {
    access.member(org, user);
    return meetings
      .findByOrganizationIdOrderByStartsAtDesc(org)
      .stream()
      .map(m -> view(m, user))
      .toList();
  }

  @Transactional(readOnly = true)
  public View get(Long id, Long user) {
    var m = meetings.findById(id).orElseThrow(ApiException::missing);
    access.member(m.organizationId, user);
    return view(m, user);
  }

  public View create(Long org, Long user, Input d) {
    access.staff(org, user);
    var m = new Meeting();
    m.organizationId = org;
    set(m, d);
    return view(meetings.save(m), user);
  }

  public View update(Long id, Long user, Input d) {
    var m = meetings.findById(id).orElseThrow(ApiException::missing);
    access.staff(m.organizationId, user);
    set(m, d);
    return view(m, user);
  }
}
