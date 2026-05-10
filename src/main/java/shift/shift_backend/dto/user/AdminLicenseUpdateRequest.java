package shift.shift_backend.dto.user;

import java.time.LocalDate;
import shift.shift_backend.domain.enums.DocumentStatus;

public record AdminLicenseUpdateRequest(
        String driverLicense,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        DocumentStatus docStatus
) {
}
