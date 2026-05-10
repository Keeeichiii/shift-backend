package shift.shift_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.Trip;
import shift.shift_backend.domain.enums.TripStatus;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findAllByUserId(Long userId);

    List<Trip> findAllByUserIdAndStatus(Long userId, TripStatus status);
}
