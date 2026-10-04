package kr.ucc.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

  public final HttpStatus status;
  public final String code;

  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static ApiException bad(String code, String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, code, message);
  }

  public static ApiException missing() {
    return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "대상을 찾을 수 없습니다.");
  }

  public static ApiException forbidden() {
    return new ApiException(
      HttpStatus.FORBIDDEN,
      "FORBIDDEN",
      "이 작업에 필요한 조직 권한이 없습니다."
    );
  }
}
