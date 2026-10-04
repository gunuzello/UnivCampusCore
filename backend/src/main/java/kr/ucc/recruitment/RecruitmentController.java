package kr.ucc.recruitment;

import jakarta.validation.Valid;
import java.util.List;
import kr.ucc.auth.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RecruitmentController {

  private final RecruitmentService service;

  public RecruitmentController(RecruitmentService service) {
    this.service = service;
  }

  @GetMapping("/organizations/{org}/recruitments")
  List<RecruitmentService.View> list(@PathVariable Long org, Authentication a) {
    return service.list(org, CurrentUser.id(a));
  }

  @PostMapping("/organizations/{org}/recruitments")
  RecruitmentService.View create(
    @PathVariable Long org,
    @Valid @RequestBody RecruitmentService.Input d,
    Authentication a
  ) {
    return service.create(org, CurrentUser.id(a), d);
  }

  @GetMapping("/recruitments/{id}")
  RecruitmentService.View get(@PathVariable Long id, Authentication a) {
    return service.get(id, CurrentUser.id(a));
  }

  @PatchMapping("/recruitments/{id}")
  RecruitmentService.View update(
    @PathVariable Long id,
    @Valid @RequestBody RecruitmentService.Input d,
    Authentication a
  ) {
    return service.update(id, CurrentUser.id(a), d);
  }

  @PatchMapping("/recruitments/{id}/status")
  RecruitmentService.View status(
    @PathVariable Long id,
    @Valid @RequestBody RecruitmentService.StatusInput d,
    Authentication a
  ) {
    return service.status(id, CurrentUser.id(a), d.status());
  }

  @PostMapping("/recruitments/{id}/applications")
  RecruitmentService.ApplicationView apply(
    @PathVariable Long id,
    @Valid @RequestBody RecruitmentService.Apply d,
    Authentication a
  ) {
    return service.apply(id, CurrentUser.id(a), d);
  }

  @GetMapping("/recruitments/{id}/applications/me")
  org.springframework.http.ResponseEntity<RecruitmentService.ApplicationView> mine(
    @PathVariable Long id,
    Authentication a
  ) {
    var application = service.mine(id, CurrentUser.id(a));
    return application == null
      ? org.springframework.http.ResponseEntity.noContent().build()
      : org.springframework.http.ResponseEntity.ok(application);
  }

  @DeleteMapping("/recruitments/{id}/applications/me")
  RecruitmentService.ApplicationView cancel(@PathVariable Long id, Authentication a) {
    return service.cancel(id, CurrentUser.id(a));
  }

  @GetMapping("/recruitments/{id}/applications")
  List<RecruitmentService.ApplicationView> applicants(@PathVariable Long id, Authentication a) {
    return service.applicants(id, CurrentUser.id(a));
  }

  @GetMapping("/recruitments/{id}/applications/{applicationId}")
  RecruitmentService.ApplicationView application(
    @PathVariable Long id,
    @PathVariable Long applicationId,
    Authentication a
  ) {
    return service.application(id, applicationId, CurrentUser.id(a));
  }

  @PatchMapping("/recruitments/{id}/applications/{applicationId}")
  RecruitmentService.ApplicationView result(
    @PathVariable Long id,
    @PathVariable Long applicationId,
    @Valid @RequestBody RecruitmentService.ApplicationStatus d,
    Authentication a
  ) {
    return service.result(id, applicationId, CurrentUser.id(a), d);
  }
}
