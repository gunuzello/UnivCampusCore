package kr.ucc.rental;

import jakarta.validation.Valid;
import java.util.List;
import kr.ucc.auth.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RentalController {

  private final RentalService service;

  public RentalController(RentalService service) {
    this.service = service;
  }

  @GetMapping("/organizations/{org}/rental-items")
  List<RentalService.ItemView> items(@PathVariable Long org, Authentication a) {
    return service.list(org, CurrentUser.id(a));
  }

  @PostMapping("/organizations/{org}/rental-items")
  RentalService.ItemView create(
    @PathVariable Long org,
    @Valid @RequestBody RentalService.Input d,
    Authentication a
  ) {
    return service.create(org, CurrentUser.id(a), d);
  }

  @PatchMapping("/rental-items/{id}")
  RentalService.ItemView update(
    @PathVariable Long id,
    @Valid @RequestBody RentalService.Input d,
    Authentication a
  ) {
    return service.update(id, CurrentUser.id(a), d);
  }

  @PostMapping("/rental-items/{id}/loans")
  RentalService.LoanView request(
    @PathVariable Long id,
    @Valid @RequestBody RentalService.Request d,
    Authentication a
  ) {
    return service.request(id, CurrentUser.id(a), d);
  }

  @GetMapping("/me/rentals")
  List<RentalService.LoanView> mine(Authentication a) {
    return service.mine(CurrentUser.id(a));
  }

  @GetMapping("/organizations/{org}/rentals")
  List<RentalService.LoanView> history(@PathVariable Long org, Authentication a) {
    return service.history(org, CurrentUser.id(a));
  }

  @PostMapping("/rentals/{id}/cancellation")
  RentalService.LoanView cancel(@PathVariable Long id, Authentication a) {
    return service.cancel(id, CurrentUser.id(a));
  }

  @PatchMapping("/rentals/{id}/status")
  RentalService.LoanView status(
    @PathVariable Long id,
    @Valid @RequestBody RentalService.Change d,
    Authentication a
  ) {
    return service.change(id, CurrentUser.id(a), d);
  }
}
