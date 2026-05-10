package shift.shift_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.SupportRequest;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    List<SupportRequest> findAllByOrderByCreatedAtDesc();
}
