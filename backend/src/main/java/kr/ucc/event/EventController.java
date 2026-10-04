package kr.ucc.event;
import kr.ucc.auth.CurrentUser;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import jakarta.validation.Valid;import java.util.List;
@RestController @RequestMapping("/api/v1") public class EventController {
 private final EventService service;public EventController(EventService service){this.service=service;}
 @GetMapping("/organizations/{org}/events") List<EventService.View> list(@PathVariable Long org,Authentication a){return service.list(org,CurrentUser.id(a));}
 @PostMapping("/organizations/{org}/events") EventService.View create(@PathVariable Long org,@Valid @RequestBody EventService.Input d,Authentication a){return service.create(org,CurrentUser.id(a),d);}
 @GetMapping("/events/{id}") EventService.View get(@PathVariable Long id,Authentication a){return service.get(id,CurrentUser.id(a));}
 @PatchMapping("/events/{id}") EventService.View update(@PathVariable Long id,@Valid @RequestBody EventService.Input d,Authentication a){return service.update(id,CurrentUser.id(a),d);}
 @PatchMapping("/events/{id}/status") EventService.View status(@PathVariable Long id,@Valid @RequestBody EventService.StatusInput d,Authentication a){return service.status(id,CurrentUser.id(a),d.status());}
 @PostMapping("/events/{id}/applications") EventService.ApplicationView apply(@PathVariable Long id,@Valid @RequestBody EventService.Apply d,Authentication a){return service.apply(id,CurrentUser.id(a),d);}
 @GetMapping("/events/{id}/applications/me") EventService.ApplicationView mine(@PathVariable Long id,Authentication a){return service.mine(id,CurrentUser.id(a));}
 @DeleteMapping("/events/{id}/applications/me") EventService.ApplicationView cancel(@PathVariable Long id,Authentication a){return service.cancel(id,CurrentUser.id(a));}
 @GetMapping("/events/{id}/applications") List<EventService.ApplicationView> applicants(@PathVariable Long id,Authentication a){return service.applicants(id,CurrentUser.id(a));}
 @PatchMapping("/events/{id}/applications/{applicationId}") EventService.ApplicationView result(@PathVariable Long id,@PathVariable Long applicationId,@Valid @RequestBody EventService.ApplicationStatus d,Authentication a){return service.result(id,applicationId,CurrentUser.id(a),d);}
 @PostMapping("/events/{id}/copies") EventService.View copy(@PathVariable Long id,@Valid @RequestBody EventService.Input d,Authentication a){var old=service.get(id,CurrentUser.id(a));return service.create(old.organizationId(),CurrentUser.id(a),d);}
}
