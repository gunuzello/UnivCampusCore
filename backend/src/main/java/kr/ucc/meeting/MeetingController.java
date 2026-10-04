package kr.ucc.meeting;

import jakarta.validation.Valid;
import java.util.List;
import kr.ucc.auth.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MeetingController {

  private final MeetingService service;

  public MeetingController(MeetingService service) {
    this.service = service;
  }

  @GetMapping("/organizations/{org}/meetings")
  List<MeetingService.View> list(@PathVariable Long org, Authentication a) {
    return service.list(org, CurrentUser.id(a));
  }

  @PostMapping("/organizations/{org}/meetings")
  MeetingService.View create(
    @PathVariable Long org,
    @Valid @RequestBody MeetingService.Input d,
    Authentication a
  ) {
    return service.create(org, CurrentUser.id(a), d);
  }

  @GetMapping("/meetings/{id}")
  MeetingService.View get(@PathVariable Long id, Authentication a) {
    return service.get(id, CurrentUser.id(a));
  }

  @PatchMapping("/meetings/{id}")
  MeetingService.View update(
    @PathVariable Long id,
    @Valid @RequestBody MeetingService.Input d,
    Authentication a
  ) {
    return service.update(id, CurrentUser.id(a), d);
  }
}
