package shift.shift_backend.dto.user;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import shift.shift_backend.domain.enums.DocumentStatus;

public record AdminLicenseUpdateRequest(
        @Size(max = 50) String driverLicense,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        DocumentStatus docStatus
) {
}
