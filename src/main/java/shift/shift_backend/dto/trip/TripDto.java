package shift.shift_backend.dto.trip;

import java.time.OffsetDateTime;
import shift.shift_backend.domain.enums.TripStatus;

public record TripDto(
        Long id,
        Long userId,
        Long vehicleId,
        TripStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer totalMinutes,
        Integer parkingMinutes,
        Integer distanceMeters,
        String startLocation,
        String endLocation
) {
}
