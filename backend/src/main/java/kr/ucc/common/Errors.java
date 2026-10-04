package kr.ucc.common;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;
@RestControllerAdvice
public class Errors {
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e){return ResponseEntity.status(e.status).body(Map.of("code",e.code,"message",e.getMessage()));}
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("code","VALIDATION_ERROR","message","입력값과 날짜 형식을 확인해 주세요."));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> duplicate(Exception e){return ResponseEntity.status(409).body(Map.of("code","CONFLICT","message","이미 등록된 정보이거나 다른 요청과 충돌했습니다."));}
}
