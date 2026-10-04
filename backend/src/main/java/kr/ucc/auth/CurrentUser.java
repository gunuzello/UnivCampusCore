package kr.ucc.auth;

import org.springframework.security.core.Authentication;

public final class CurrentUser {

  private CurrentUser() {}

  public static long id(Authentication authentication) {
    return Long.parseLong(authentication.getName());
  }
}
