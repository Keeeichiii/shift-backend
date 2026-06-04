package shift.shift_backend.dto.user;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import shift.shift_backend.domain.enums.DocumentStatus;

public record UserDto(
        Long id,
        Integer regionId,
        String username,
        LocalDate registrationDate,
        String firstName,
        String lastName,
        String personalIdNumber,
        String driverLicense,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        DocumentStatus docStatus,
        String profileName,
        String bio,
        OffsetDateTime lastActivity,
        String phone
) {
}
