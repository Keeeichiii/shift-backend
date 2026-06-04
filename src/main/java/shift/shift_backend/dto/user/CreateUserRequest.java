package shift.shift_backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateUserRequest(
        Integer regionId,
        @NotBlank @Size(max = 45) String username,
        @NotBlank @Size(max = 45) String firstName,
        @NotBlank @Size(max = 45) String lastName,
        @Size(max = 14) String personalIdNumber,
        @NotBlank @Size(max = 50) String driverLicense,
        @NotNull LocalDate licenseExpiresAt,
        @Size(max = 45) String profileName,
        @Size(max = 300) String bio,
        @Size(max = 30) String phone
) {
}
