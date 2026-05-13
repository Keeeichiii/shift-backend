package shift.shift_backend.dto.auth;

import java.time.LocalDate;
import java.util.List;
import shift.shift_backend.domain.enums.DocumentStatus;

public record AuthUserResponse(
        Long id,
        String username,
        String email,
        List<String> roles,
        DocumentStatus docStatus,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        String driverLicense
) {
}
