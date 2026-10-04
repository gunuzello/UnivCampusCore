package kr.ucc.common;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController public class HealthController { @GetMapping("/api/v1/health") Map<String,String> health(){return Map.of("status","UP","service","UCC");} }
