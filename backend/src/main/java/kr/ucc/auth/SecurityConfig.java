package kr.ucc.auth;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityContextRepository securityContextRepository(){return new HttpSessionSecurityContextRepository();}
 @Bean SecurityFilterChain filter(HttpSecurity http,SecurityContextRepository contexts)throws Exception{
  var csrf=new HttpSessionCsrfTokenRepository();csrf.setHeaderName("X-CSRF-TOKEN");
  return http.securityContext(c->c.securityContextRepository(contexts))
   .csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
   .authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/csrf","/api/v1/auth/signup","/api/v1/auth/login","/api/v1/health","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((q,s,x)->{s.setStatus(401);s.setContentType("application/json;charset=UTF-8");s.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"로그인이 필요합니다.\"}");})
    .accessDeniedHandler((q,s,x)->{s.setStatus(403);s.setContentType("application/json;charset=UTF-8");s.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"권한 또는 보안 토큰을 확인해 주세요.\"}");}))
   .logout(l->l.logoutUrl("/api/v1/auth/logout").logoutSuccessHandler((q,s,a)->s.setStatus(204))).build();
 }
}
