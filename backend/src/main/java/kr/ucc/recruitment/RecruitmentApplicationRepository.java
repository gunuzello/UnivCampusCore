package kr.ucc.recruitment;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentApplicationRepository
  extends JpaRepository<RecruitmentApplication, Long>
{
  Optional<RecruitmentApplication> findByRecruitmentIdAndUserId(Long recruitmentId, Long userId);
  List<RecruitmentApplication> findByRecruitmentIdOrderBySubmittedAtAsc(Long recruitmentId);
  List<RecruitmentApplication> findByUserIdOrderBySubmittedAtDesc(Long userId);
  long countByRecruitmentIdAndStatusNot(Long recruitmentId, RecruitmentApplication.Status status);
}
