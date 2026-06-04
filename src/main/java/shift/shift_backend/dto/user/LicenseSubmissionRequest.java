package shift.shift_backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LicenseSubmissionRequest(
        @NotBlank @Size(max = 2_800_000) String frontImageData,
        @NotBlank @Size(max = 2_800_000) String backImageData,
        @NotBlank @Size(max = 2_800_000) String passportMainImageData
) {
}
