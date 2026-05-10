package shift.shift_backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import shift.shift_backend.domain.enums.VehicleStatus;

@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false)
    private Integer brandId;

    @Column(name = "fleet_partner_id")
    private Integer fleetPartnerId;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(name = "telematics_device_id", unique = true, length = 100)
    private String telematicsDeviceId;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "base_rate_per_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRatePerMin;

    @Column(name = "parking_rate_per_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal parkingRatePerMin;

    @Column(length = 300)
    private String description;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "vehicle_status")
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(name = "fuel_or_battery_level")
    private Short fuelOrBatteryLevel;

    @Column(name = "current_location", columnDefinition = "geo_point")
    private String currentLocation;

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
        if (averageRating == null) {
            averageRating = BigDecimal.valueOf(5.00);
        }
        if (parkingRatePerMin == null) {
            parkingRatePerMin = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
