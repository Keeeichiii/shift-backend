package shift.shift_backend.dto.auth;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String username,
        String email,
        List<String> roles
) {
}
