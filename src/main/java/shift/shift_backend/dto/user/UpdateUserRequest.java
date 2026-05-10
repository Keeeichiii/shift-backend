package shift.shift_backend.dto.user;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import shift.shift_backend.domain.enums.DocumentStatus;

public record UpdateUserRequest(
        Integer regionId,
        @Size(max = 45) String firstName,
        @Size(max = 45) String lastName,
        @Size(max = 45) String profileName,
        @Size(max = 255) String avatarUrl,
        @Size(max = 300) String bio,
        @Size(max = 30) String phone,
        @Size(max = 50) String driverLicense,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        DocumentStatus docStatus
) {
}
