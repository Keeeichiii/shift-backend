package shift.shift_backend.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shift.shift_backend.domain.entity.VehicleCard;

public interface VehicleCardRepository extends JpaRepository<VehicleCard, Long> {

    List<VehicleCard> findAllByPublishedTrueOrderByUpdatedAtDesc();

    Optional<VehicleCard> findBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from VehicleCard c where c.id = :id")
    Optional<VehicleCard> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from VehicleCard c where c.slug = :slug")
    Optional<VehicleCard> findBySlugForUpdate(@Param("slug") String slug);
}
