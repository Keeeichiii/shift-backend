package shift.shift_backend.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import shift.shift_backend.domain.enums.DocumentStatus;

public record AdminUserManagementRequest(
        @Size(max = 45) String username,
        @Email @Size(max = 100) String email,
        @Size(min = 6, max = 100) String password,
        List<String> roles,
        Integer regionId,
        @Size(max = 45) String firstName,
        @Size(max = 45) String lastName,
        @Size(max = 45) String profileName,
        @Size(max = 300) String bio,
        @Size(max = 30) String phone,
        @Size(max = 50) String driverLicense,
        LocalDate licenseExpiresAt,
        LocalDate drivingBanUntil,
        DocumentStatus docStatus
) {
}
