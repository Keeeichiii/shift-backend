package shift.shift_backend.dto.trip;

import jakarta.validation.constraints.NotNull;

public record CreateTripRequest(
        @NotNull Long vehicleId,
        String startLocation
) {
}
