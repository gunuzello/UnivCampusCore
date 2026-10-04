package kr.ucc.meeting;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingResponseRepository extends JpaRepository<MeetingResponse, Long> {
  List<MeetingResponse> findByMeetingId(Long meetingId);
  Optional<MeetingResponse> findByMeetingIdAndUserId(Long meetingId, Long userId);
  void deleteByMeetingId(Long meetingId);
  void deleteByMeetingIdAndUserId(Long meetingId, Long userId);
}
