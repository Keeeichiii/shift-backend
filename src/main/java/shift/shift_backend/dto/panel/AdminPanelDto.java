package shift.shift_backend.dto.panel;

import java.util.List;
import shift.shift_backend.dto.support.PanelSupportRequestDto;
import shift.shift_backend.dto.vehicle.VehicleDto;

public record AdminPanelDto(
        long totalUsers,
        long activeUsers,
        long pendingModeration,
        long approvedUsers,
        List<PanelUserReviewDto> users,
        List<PanelSupportRequestDto> supportRequests,
        List<PanelLongBookingOrderDto> longBookingOrdersPending,
        List<PanelLongBookingOrderDto> longBookingOrdersConfirmed,
        List<VehicleDto> bookedFleetVehicles
) {
}
