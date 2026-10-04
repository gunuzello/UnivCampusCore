package kr.ucc.meeting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import kr.ucc.organization.OrganizationAccess;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings/{id}/responses")
@Transactional
public class MeetingResponseController {

  private final MeetingRepository meetings;
  private final MeetingResponseRepository responses;
  private final OrganizationAccess access;

  public MeetingResponseController(
    MeetingRepository meetings,
    MeetingResponseRepository responses,
    OrganizationAccess access
  ) {
    this.meetings = meetings;
    this.responses = responses;
    this.access = access;
  }

  public record Input(@NotNull MeetingResponse.Status status) {}

  public record View(Long userId, MeetingResponse.Status status, Instant updatedAt) {}

  private View view(MeetingResponse r) {
    return new View(r.userId, r.status, r.updatedAt);
  }

  @GetMapping
  @Transactional(readOnly = true)
  List<View> list(@PathVariable Long id, Authentication a) {
    var m = meetings.findById(id).orElseThrow(ApiException::missing);
    access.member(m.organizationId, CurrentUser.id(a));
    return responses
      .findByMeetingId(id)
      .stream()
      .filter(r -> m.attendees.contains(r.userId))
      .map(this::view)
      .toList();
  }

  @PutMapping("/me")
  View respond(@PathVariable Long id, @Valid @RequestBody Input input, Authentication a) {
    var m = meetings.lockById(id).orElseThrow(ApiException::missing);
    Long user = CurrentUser.id(a);
    access.member(m.organizationId, user);
    if (!m.attendees.contains(user)) throw ApiException.forbidden();
    if (!Instant.now().isBefore(m.startsAt)) throw ApiException.bad(
      "RESPONSE_CLOSED",
      "회의 시작 후에는 참석 의사를 변경할 수 없습니다."
    );
    var r = responses.findByMeetingIdAndUserId(id, user).orElseGet(MeetingResponse::new);
    r.meetingId = id;
    r.userId = user;
    r.status = input.status();
    r.updatedAt = Instant.now();
    return view(responses.save(r));
  }
}
