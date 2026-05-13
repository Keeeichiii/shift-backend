package shift.shift_backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import shift.shift_backend.domain.enums.LongBookingOrderStatus;

@Getter
@Setter
@Entity
@Table(name = "long_booking_orders")
public class LongBookingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_card_id", nullable = false)
    private VehicleCard vehicleCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LongBookingOrderStatus status = LongBookingOrderStatus.PENDING;

    @Column(name = "customer_note", columnDefinition = "text")
    private String customerNote;

    @Column(name = "requested_start_at", nullable = false)
    private OffsetDateTime requestedStartAt;

    @Column(name = "requested_end_at", nullable = false)
    private OffsetDateTime requestedEndAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
