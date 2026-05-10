package shift.shift_backend.dto.support;

import java.time.OffsetDateTime;

public record SupportRequestDto(
        Long id,
        Long userId,
        String contactChannel,
        String subject,
        String message,
        OffsetDateTime createdAt
) {
}
