package shift.shift_backend.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportRequest(
        @NotBlank @Size(max = 40) String contactChannel,
        @NotBlank @Size(max = 160) String contactValue,
        @NotBlank @Size(max = 160) String subject,
        @NotBlank @Size(max = 4000) String message
) {
}
