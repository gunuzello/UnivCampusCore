package kr.ucc.recruitment;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication,Long>{
 Optional<RecruitmentApplication> findByRecruitmentIdAndUserId(Long recruitmentId,Long userId);
 List<RecruitmentApplication> findByRecruitmentIdOrderBySubmittedAtAsc(Long recruitmentId);
 long countByRecruitmentIdAndStatusNot(Long recruitmentId,RecruitmentApplication.Status status);
}
