package shift.shift_backend.dto.panel;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import shift.shift_backend.domain.enums.DocumentStatus;

public record PanelUserReviewDto(
        Long id,
        String username,
        String fullName,
        String email,
        List<String> roles,
        LocalDate registrationDate,
        String driverLicense,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        String licenseFrontImage,
        String licenseBackImage,
        String passportMainImage,
        OffsetDateTime licenseSubmittedAt,
        DocumentStatus docStatus,
        OffsetDateTime lastActivity,
        boolean eligibleForApproval,
        String moderationNote
) {
}
