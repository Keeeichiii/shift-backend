package shift.shift_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shift.shift_backend.domain.entity.Vehicle;
import shift.shift_backend.domain.enums.VehicleStatus;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVin(String vin);

    List<Vehicle> findAllByStatus(VehicleStatus status);
}
