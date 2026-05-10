package shift.shift_backend.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import shift.shift_backend.domain.enums.VehicleStatus;

public record VehicleDto(
        Long id,
        Integer brandId,
        Integer fleetPartnerId,
        String vin,
        String licensePlate,
        String telematicsDeviceId,
        LocalDate releaseDate,
        BigDecimal baseRatePerMin,
        BigDecimal parkingRatePerMin,
        String description,
        BigDecimal averageRating,
        VehicleStatus status,
        Short fuelOrBatteryLevel,
        String currentLocation
) {
}
