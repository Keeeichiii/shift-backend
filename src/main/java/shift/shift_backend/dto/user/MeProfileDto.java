package shift.shift_backend.dto.user;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import shift.shift_backend.domain.enums.DocumentStatus;

public record MeProfileDto(
        Long id,
        String email,
        List<String> roles,
        Integer regionId,
        String username,
        LocalDate registrationDate,
        String firstName,
        String lastName,
        String personalIdNumber,
        String driverLicense,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        String licenseFrontImage,
        String licenseBackImage,
        String passportMainImage,
        OffsetDateTime licenseSubmittedAt,
        DocumentStatus docStatus,
        String profileName,
        String bio,
        OffsetDateTime lastActivity,
        String phone
) {
}
