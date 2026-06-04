package shift.shift_backend.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shift.shift_backend.domain.entity.LongBookingOrder;
import shift.shift_backend.domain.enums.LongBookingOrderStatus;

public interface LongBookingOrderRepository extends JpaRepository<LongBookingOrder, Long> {

    @EntityGraph(attributePaths = "vehicleCard")
    List<LongBookingOrder> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "vehicleCard")
    List<LongBookingOrder> findAllByStatusOrderByCreatedAtDesc(LongBookingOrderStatus status);

    @Query("""
            SELECT o FROM LongBookingOrder o
            WHERE o.vehicleCard.id = :vehicleCardId
            AND o.status IN :statuses
            AND o.requestedStartAt < :rangeEnd
            AND o.requestedEndAt > :rangeStart
            """)
    List<LongBookingOrder> findOverlappingForVehicleCard(
            @Param("vehicleCardId") Long vehicleCardId,
            @Param("statuses") Collection<LongBookingOrderStatus> statuses,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    @Query("""
            SELECT o FROM LongBookingOrder o
            WHERE o.id <> :excludedOrderId
            AND o.vehicleCard.id = :vehicleCardId
            AND o.status IN :statuses
            AND o.requestedStartAt < :rangeEnd
            AND o.requestedEndAt > :rangeStart
            """)
    List<LongBookingOrder> findOverlappingForVehicleCardExcludingOrder(
            @Param("excludedOrderId") Long excludedOrderId,
            @Param("vehicleCardId") Long vehicleCardId,
            @Param("statuses") Collection<LongBookingOrderStatus> statuses,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );
}
