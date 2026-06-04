package shift.shift_backend.dto.longbooking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import shift.shift_backend.domain.enums.LongBookingOrderStatus;

public record LongBookingOrderDto(
        Long id,
        LongBookingOrderStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime requestedStartAt,
        OffsetDateTime requestedEndAt,
        String customerNote,
        BigDecimal estimatedPrice,
        String vehicleTitle,
        String vehicleSlug,
        String vehicleImagePath,
        String vehicleCategory
) {
}
