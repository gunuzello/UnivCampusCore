package kr.ucc.event;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import kr.ucc.common.ApiException;
import kr.ucc.notification.Notifications;
import kr.ucc.organization.*;
import kr.ucc.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventService {

  private final EventRepository repository;
  private final EventApplicationRepository applications;
  private final OrganizationAccess access;
  private final OrganizationRepository organizations;
  private final UserRepository users;
  private final Notifications notifications;

  public EventService(
    EventRepository repository,
    EventApplicationRepository applications,
    OrganizationAccess access,
    OrganizationRepository organizations,
    UserRepository users,
    Notifications notifications
  ) {
    this.repository = repository;
    this.applications = applications;
    this.access = access;
    this.organizations = organizations;
    this.users = users;
    this.notifications = notifications;
  }

  public record Input(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 20000) String description,
    @NotNull Instant opensAt,
    @NotNull Instant closesAt,
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    @NotBlank @Size(max = 200) String location,
    @Min(1) @Max(100000) int capacity,
    @NotNull @Size(max = 20) List<@NotBlank @Size(max = 500) String> questions
  ) {}

  public record StatusInput(@NotNull Event.Status status) {}

  public record Apply(@NotNull @Size(max = 20) List<@NotBlank @Size(max = 5000) String> answers) {}

  public record ApplicationStatus(@NotNull EventApplication.Status status) {}

  public record View(
    Long id,
    Long organizationId,
    String title,
    String description,
    Instant opensAt,
    Instant closesAt,
    Instant startsAt,
    Instant endsAt,
    String location,
    int capacity,
    Event.Status status,
    List<String> questions,
    long applicationCount,
    boolean canManage
  ) {}

  public record ApplicationView(
    Long id,
    Long userId,
    String name,
    String email,
    String department,
    String studentNumber,
    EventApplication.Status status,
    List<String> answers,
    Instant submittedAt
  ) {}

  private boolean staff(Long org, Long user) {
    try {
      access.staff(org, user);
      return true;
    } catch (ApiException ex) {
      return false;
    }
  }

  private View view(Event e, Long user) {
    return new View(
      e.id,
      e.organizationId,
      e.title,
      e.description,
      e.opensAt,
      e.closesAt,
      e.startsAt,
      e.endsAt,
      e.location,
      e.capacity,
      e.status,
      List.copyOf(e.questions),
      applications.countByEventIdAndStatusNot(e.id, EventApplication.Status.CANCELLED),
      staff(e.organizationId, user)
    );
  }

  private ApplicationView applicationView(EventApplication a) {
    return new ApplicationView(
      a.id,
      a.userId,
      a.name,
      a.email,
      a.department,
      a.studentNumber,
      a.status,
      List.copyOf(a.answers),
      a.submittedAt
    );
  }

  private Event visible(Long id, Long user) {
    var e = repository.findById(id).orElseThrow(ApiException::missing);
    if (
      e.status == Event.Status.DRAFT && !staff(e.organizationId, user)
    ) throw ApiException.missing();
    return e;
  }

  @Transactional(readOnly = true)
  public List<View> list(Long org, Long user) {
    if (!organizations.existsById(org)) throw ApiException.missing();
    return repository
      .findByOrganizationIdOrderByCreatedAtDesc(org)
      .stream()
      .filter(e -> e.status != Event.Status.DRAFT || staff(org, user))
      .map(e -> view(e, user))
      .toList();
  }

  @Transactional(readOnly = true)
  public View get(Long id, Long user) {
    return view(visible(id, user), user);
  }

  private void set(Event e, Input d) {
    if (!d.opensAt().isBefore(d.closesAt())) throw ApiException.bad(
      "INVALID_PERIOD",
      "신청 종료 시각은 시작 이후여야 합니다."
    );
    if (
      !d.startsAt().isBefore(d.endsAt()) || d.closesAt().isAfter(d.startsAt())
    ) throw ApiException.bad(
      "INVALID_PERIOD",
      "신청은 행사 시작 이전에 종료되어야 하며, 행사 종료는 시작 이후여야 합니다."
    );
    e.title = d.title().trim();
    e.description = d.description();
    e.opensAt = d.opensAt();
    e.closesAt = d.closesAt();
    e.startsAt = d.startsAt();
    e.endsAt = d.endsAt();
    e.location = d.location();
    e.capacity = d.capacity();
    e.questions = new ArrayList<>(d.questions());
  }

  public View create(Long org, Long user, Input d) {
    access.staff(org, user);
    var e = new Event();
    e.organizationId = org;
    set(e, d);
    return view(repository.save(e), user);
  }

  public View update(Long id, Long user, Input d) {
    var e = repository.lockById(id).orElseThrow(ApiException::missing);
    access.staff(e.organizationId, user);
    if (
      e.status == Event.Status.COMPLETED || e.status == Event.Status.CANCELLED
    ) throw ApiException.bad("ARCHIVED_ACTIVITY", "종료된 활동은 변경할 수 없습니다.");
    if (
      e.status != Event.Status.DRAFT && !e.questions.equals(d.questions())
    ) throw ApiException.bad("QUESTIONS_LOCKED", "공개한 뒤에는 질문을 변경할 수 없습니다.");
    if (
      applications.countByEventIdAndStatusNot(id, EventApplication.Status.CANCELLED) > d.capacity()
    ) throw ApiException.bad("CAPACITY_TOO_SMALL", "현재 신청 인원보다 정원을 줄일 수 없습니다.");
    set(e, d);
    for (var a : applications.findByEventIdOrderBySubmittedAtAsc(id)) {
      if (a.status != EventApplication.Status.CANCELLED) notifications.send(
        a.userId,
        e.title + " 내용이 변경되었습니다.",
        "/events/" + id
      );
    }
    return view(e, user);
  }

  public View status(Long id, Long user, Event.Status next) {
    var e = repository.lockById(id).orElseThrow(ApiException::missing);
    access.staff(e.organizationId, user);
    if (e.status == next) return view(e, user);
    boolean allowed =
      (e.status == Event.Status.DRAFT &&
        (next == Event.Status.PUBLISHED || next == Event.Status.CANCELLED)) ||
      (e.status == Event.Status.PUBLISHED &&
        (next == Event.Status.CLOSED || next == Event.Status.CANCELLED)) ||
      (e.status == Event.Status.CLOSED &&
        (next == Event.Status.COMPLETED || next == Event.Status.CANCELLED));
    if (next == Event.Status.COMPLETED && Instant.now().isBefore(e.endsAt)) throw ApiException.bad(
      "EVENT_NOT_ENDED",
      "행사 종료 시각 이후에 완료할 수 있습니다."
    );
    if (!allowed) throw ApiException.bad("INVALID_TRANSITION", "허용되지 않은 상태 변경입니다.");
    e.status = next;
    if (
      next == Event.Status.CANCELLED
    ) for (var a : applications.findByEventIdOrderBySubmittedAtAsc(id)) {
      if (a.status != EventApplication.Status.CANCELLED) notifications.send(
        a.userId,
        e.title + " 활동이 취소되었습니다.",
        "/events/" + id
      );
    }
    return view(e, user);
  }

  public ApplicationView apply(Long id, Long user, Apply d) {
    var e = repository.lockById(id).orElseThrow(ApiException::missing);
    var now = Instant.now();
    if (e.status != Event.Status.PUBLISHED) throw ApiException.bad(
      "NOT_OPEN",
      "현재 신청할 수 없는 활동입니다."
    );
    if (now.isBefore(e.opensAt)) throw ApiException.bad("NOT_STARTED", "아직 신청 시작 전입니다.");
    if (!now.isBefore(e.closesAt)) throw ApiException.bad(
      "PERIOD_ENDED",
      "신청 기간이 종료되었습니다."
    );
    if (d.answers().size() != e.questions.size()) throw ApiException.bad(
      "INVALID_ANSWERS",
      "모든 질문에 답변해 주세요."
    );
    var prior = applications.findByEventIdAndUserId(id, user);
    if (
      prior.isPresent() && prior.get().status != EventApplication.Status.CANCELLED
    ) throw new ApiException(HttpStatus.CONFLICT, "ALREADY_APPLIED", "이미 신청한 활동입니다.");
    if (
      applications.countByEventIdAndStatusNot(id, EventApplication.Status.CANCELLED) >= e.capacity
    ) throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_FULL", "행사 정원이 마감되었습니다.");
    var u = users.findById(user).orElseThrow(ApiException::missing);
    var a = prior.orElseGet(EventApplication::new);
    a.eventId = id;
    a.userId = user;
    a.name = u.name;
    a.email = u.email;
    a.department = u.department;
    a.studentNumber = u.studentNumber;
    a.answers = new ArrayList<>(d.answers());
    a.status = EventApplication.Status.REGISTERED;
    a.submittedAt = now;
    applications.save(a);
    notifications.send(user, e.title + " 신청이 완료되었습니다.", "/events/" + id);
    return applicationView(a);
  }

  @Transactional(readOnly = true)
  public ApplicationView mine(Long id, Long user) {
    visible(id, user);
    return applications.findByEventIdAndUserId(id, user).map(this::applicationView).orElse(null);
  }

  public ApplicationView cancel(Long id, Long user) {
    var e = repository.lockById(id).orElseThrow(ApiException::missing);
    if (
      e.status != Event.Status.PUBLISHED || !Instant.now().isBefore(e.closesAt)
    ) throw ApiException.bad("CANCELLATION_CLOSED", "신청 기간 내에만 취소할 수 있습니다.");
    var a = applications.findByEventIdAndUserId(id, user).orElseThrow(ApiException::missing);
    a.status = EventApplication.Status.CANCELLED;
    notifications.send(user, e.title + " 신청을 취소했습니다.", "/events/" + id);
    return applicationView(a);
  }

  @Transactional(readOnly = true)
  public List<ApplicationView> applicants(Long id, Long user) {
    var e = visible(id, user);
    access.staff(e.organizationId, user);
    return applications
      .findByEventIdOrderBySubmittedAtAsc(id)
      .stream()
      .map(this::applicationView)
      .toList();
  }

  public ApplicationView result(Long id, Long applicationId, Long user, ApplicationStatus d) {
    var e = repository.lockById(id).orElseThrow(ApiException::missing);
    access.staff(e.organizationId, user);
    if (e.status == Event.Status.CANCELLED) throw ApiException.bad(
      "INVALID_STATE",
      "취소된 활동입니다."
    );
    var a = applications.findById(applicationId).orElseThrow(ApiException::missing);
    if (!a.eventId.equals(id)) throw ApiException.missing();
    if (a.status == EventApplication.Status.CANCELLED) throw ApiException.bad(
      "INVALID_STATE",
      "취소한 신청서는 변경할 수 없습니다."
    );
    if (e.status != Event.Status.CLOSED) throw ApiException.bad(
      "INVALID_STATE",
      "신청을 마감한 행사에서 참가 상태를 관리해 주세요."
    );
    if (
      d.status() != EventApplication.Status.ATTENDED &&
      d.status() != EventApplication.Status.ABSENT &&
      d.status() != EventApplication.Status.REGISTERED
    ) throw ApiException.bad("INVALID_STATE", "참가 또는 불참 상태를 선택해 주세요.");
    a.status = d.status();
    notifications.send(a.userId, e.title + " 신청 상태가 변경되었습니다.", "/events/" + id);
    return applicationView(a);
  }

  @Transactional(readOnly = true)
  public List<kr.ucc.application.MyApplicationView> myApplications(Long user) {
    return applications
      .findByUserIdOrderBySubmittedAtDesc(user)
      .stream()
      .map(a -> {
        var e = repository.findById(a.eventId).orElseThrow(ApiException::missing);
        return new kr.ucc.application.MyApplicationView(
          "event-" + a.id,
          "EVENT",
          e.title,
          a.status.name(),
          "/events/" + e.id,
          a.submittedAt
        );
      })
      .toList();
  }
}
