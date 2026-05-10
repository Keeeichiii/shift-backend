package shift.shift_backend.dto.panel;

import java.util.List;
import shift.shift_backend.dto.support.PanelSupportRequestDto;

public record AdminPanelDto(
        long totalUsers,
        long activeUsers,
        long pendingModeration,
        long approvedUsers,
        List<PanelUserReviewDto> users,
        List<PanelSupportRequestDto> supportRequests
) {
}
