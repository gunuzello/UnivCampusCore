package kr.ucc.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.ucc.auth.CurrentUser;
import kr.ucc.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class ProfileController {

  private final UserRepository users;

  public ProfileController(UserRepository users) {
    this.users = users;
  }

  public record Update(
    @NotBlank @Size(max = 80) String name,
    @Size(max = 120) String department,
    @Size(max = 40) String studentNumber
  ) {}

  @GetMapping
  User.Profile me(Authentication a) {
    return users.findById(CurrentUser.id(a)).orElseThrow(ApiException::missing).profile();
  }

  @PatchMapping
  @Transactional
  User.Profile update(Authentication a, @Valid @RequestBody Update d) {
    User u = users.findById(CurrentUser.id(a)).orElseThrow(ApiException::missing);
    u.name = d.name().trim();
    u.department = d.department();
    u.studentNumber = d.studentNumber();
    return u.profile();
  }
}
