package shift.shift_backend.dto.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNewsRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 180) String slug,
        @NotBlank @Size(max = 500) String summary,
        @NotBlank @Size(max = 12000) String content,
        boolean published
) {
}
