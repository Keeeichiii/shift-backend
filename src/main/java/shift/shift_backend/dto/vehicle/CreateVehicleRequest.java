package shift.shift_backend.dto.vehicle;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateVehicleRequest(
        @NotNull Integer brandId,
        Integer fleetPartnerId,
        @NotBlank @Size(min = 17, max = 17) String vin,
        @NotBlank @Size(max = 20) String licensePlate,
        @Size(max = 100) String telematicsDeviceId,
        @NotNull LocalDate releaseDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal baseRatePerMin,
        @DecimalMin(value = "0.00") BigDecimal parkingRatePerMin,
        @Size(max = 300) String description
) {
}
