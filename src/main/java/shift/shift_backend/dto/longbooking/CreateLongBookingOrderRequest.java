package shift.shift_backend.dto.longbooking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateLongBookingOrderRequest(
        @NotBlank @Size(max = 120) String vehicleCardSlug,
        @NotNull OffsetDateTime requestedStartAt,
        @NotNull OffsetDateTime requestedEndAt,
        @Size(max = 4000) String customerNote
) {
}
