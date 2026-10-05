package kr.ucc.common;

import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {

  private final Environment environment;

  public HealthController(Environment environment) {
    this.environment = environment;
  }

  @GetMapping("/api/v1/health")
  Map<String, Object> health() {
    return Map.of("status", "UP", "service", "UCC", "demo", environment.matchesProfiles("local"));
  }
}
