package shift.shift_backend.dto.news;

import java.time.OffsetDateTime;

public record NewsDto(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        boolean published,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
