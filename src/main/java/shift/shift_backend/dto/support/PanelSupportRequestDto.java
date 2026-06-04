package shift.shift_backend.dto.support;

import java.time.OffsetDateTime;

public record PanelSupportRequestDto(
        Long id,
        Long userId,
        String username,
        String fullName,
        String email,
        String contactChannel,
        String contactValue,
        String subject,
        String message,
        OffsetDateTime createdAt
) {
}
