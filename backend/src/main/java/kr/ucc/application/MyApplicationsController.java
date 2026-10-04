package kr.ucc.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kr.ucc.auth.CurrentUser;
import kr.ucc.event.EventService;
import kr.ucc.recruitment.RecruitmentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/applications")
public class MyApplicationsController {

  private final EventService events;
  private final RecruitmentService recruitments;

  public MyApplicationsController(EventService events, RecruitmentService recruitments) {
    this.events = events;
    this.recruitments = recruitments;
  }

  @GetMapping
  List<MyApplicationView> list(Authentication a) {
    var out = new ArrayList<MyApplicationView>();
    out.addAll(events.myApplications(CurrentUser.id(a)));
    out.addAll(recruitments.myApplications(CurrentUser.id(a)));
    return out
      .stream()
      .sorted(java.util.Comparator.comparing(MyApplicationView::submittedAt).reversed())
      .toList();
  }
}
