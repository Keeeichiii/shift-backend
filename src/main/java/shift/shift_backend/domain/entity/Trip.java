package shift.shift_backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import shift.shift_backend.domain.enums.TripStatus;

@Getter
@Setter
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "trip_status")
    private TripStatus status = TripStatus.RESERVED;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes = 0;

    @Column(name = "parking_minutes", nullable = false)
    private Integer parkingMinutes = 0;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters = 0;

    @Column(name = "start_location", columnDefinition = "geo_point")
    private String startLocation;

    @Column(name = "end_location", columnDefinition = "geo_point")
    private String endLocation;
}
