package shift.shift_backend.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        Integer regionId,
        @Size(max = 45) String firstName,
        @Size(max = 45) String lastName,
        @Size(max = 14) String personalIdNumber,
        @Size(max = 45) String profileName,
        @Size(max = 255) String avatarUrl,
        @Size(max = 300) String bio,
        @Size(max = 30) String phone
) {
}
