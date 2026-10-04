package kr.ucc.archive;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverNoteRepository extends JpaRepository<HandoverNote, Long> {
  List<HandoverNote> findByOrganizationIdOrderByUpdatedAtDesc(Long organizationId);
}
