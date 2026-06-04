package shift.shift_backend.dto.panel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import shift.shift_backend.domain.enums.LongBookingOrderStatus;

public record PanelLongBookingOrderDto(
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
        Long userId,
        String username,
        String userEmail
) {
}
