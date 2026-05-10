package shift.shift_backend.dto.vehicle;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import shift.shift_backend.domain.enums.VehicleStatus;

public record UpdateVehicleRequest(
        Integer fleetPartnerId,
        @DecimalMin(value = "0.01") BigDecimal baseRatePerMin,
        @DecimalMin(value = "0.00") BigDecimal parkingRatePerMin,
        @Size(max = 300) String description,
        @Min(0) @Max(100) Short fuelOrBatteryLevel,
        VehicleStatus status,
        String currentLocation
) {
}
