package shift.shift_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.VehicleCard;

public interface VehicleCardRepository extends JpaRepository<VehicleCard, Long> {

    List<VehicleCard> findAllByPublishedTrueOrderByUpdatedAtDesc();

    Optional<VehicleCard> findBySlug(String slug);
}
