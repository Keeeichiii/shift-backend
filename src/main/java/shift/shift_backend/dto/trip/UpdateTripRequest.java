package shift.shift_backend.dto.trip;

import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import shift.shift_backend.domain.enums.TripStatus;

public record UpdateTripRequest(
        TripStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Min(0) Integer totalMinutes,
        @Min(0) Integer parkingMinutes,
        @Min(0) Integer distanceMeters,
        String endLocation
) {
}
