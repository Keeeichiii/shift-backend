package shift.shift_backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Integer> {
    Optional<Region> findByCode(String code);
}
