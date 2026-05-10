package shift.shift_backend.dto.user;

import jakarta.validation.constraints.NotBlank;

public record LicenseSubmissionRequest(
        @NotBlank String frontImageData,
        @NotBlank String backImageData,
        @NotBlank String passportMainImageData
) {
}
