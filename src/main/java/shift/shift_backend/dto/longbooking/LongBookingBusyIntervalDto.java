package shift.shift_backend.dto.longbooking;

import java.time.OffsetDateTime;

public record LongBookingBusyIntervalDto(OffsetDateTime startAt, OffsetDateTime endAt) {
}
