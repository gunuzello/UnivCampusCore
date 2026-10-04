package kr.ucc.auth;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import kr.ucc.common.ApiException;
import kr.ucc.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final SecurityContextRepository contexts;

  public AuthController(
    UserRepository users,
    PasswordEncoder passwords,
    SecurityContextRepository contexts
  ) {
    this.users = users;
    this.passwords = passwords;
    this.contexts = contexts;
  }

  public record Signup(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 80) String name,
    @Size(max = 120) String department,
    @Size(max = 40) String studentNumber
  ) {}

  public record Login(@NotBlank @Email String email, @NotBlank String password) {}

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  User.Profile signup(@Valid @RequestBody Signup d) {
    if (
      d.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
    ) throw ApiException.bad(
      "PASSWORD_TOO_LONG",
      "비밀번호는 UTF-8 기준 72바이트 이내로 입력해 주세요."
    );
    String email = d.email().trim().toLowerCase(Locale.ROOT);
    if (users.findByEmail(email).isPresent()) throw new ApiException(
      HttpStatus.CONFLICT,
      "EMAIL_IN_USE",
      "이미 가입한 이메일입니다."
    );
    return users
      .save(
        new User(
          email,
          passwords.encode(d.password()),
          d.name().trim(),
          d.department(),
          d.studentNumber()
        )
      )
      .profile();
  }

  @PostMapping("/login")
  User.Profile login(
    @Valid @RequestBody Login d,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    User user = users
      .findByEmail(d.email().trim().toLowerCase(Locale.ROOT))
      .orElseThrow(() ->
        new ApiException(
          HttpStatus.UNAUTHORIZED,
          "INVALID_CREDENTIALS",
          "이메일 또는 비밀번호를 확인해 주세요."
        )
      );
    if (!passwords.matches(d.password(), user.passwordHash)) throw new ApiException(
      HttpStatus.UNAUTHORIZED,
      "INVALID_CREDENTIALS",
      "이메일 또는 비밀번호를 확인해 주세요."
    );
    request.getSession();
    request.changeSessionId();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
      new UsernamePasswordAuthenticationToken(user.id.toString(), null, List.of())
    );
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    return user.profile();
  }
}
