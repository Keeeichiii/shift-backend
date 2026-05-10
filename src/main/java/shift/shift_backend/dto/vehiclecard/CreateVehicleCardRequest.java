package shift.shift_backend.dto.vehiclecard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateVehicleCardRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 120) String slug,
        @NotBlank @Size(max = 80) String category,
        boolean wrapped,
        @NotBlank @Size(max = 300) String imagePath,
        @NotNull @DecimalMin(value = "0.01") BigDecimal pricePerMinute,
        @Size(max = 30) String badge,
        @Size(max = 500) String shortDescription,
        @Size(max = 1200) String detailDescription,
        @Size(max = 80) String transmission,
        @Size(max = 80) String fuelType,
        @Size(max = 120) String engine,
        String conditionsText,
        String featuresText,
        String minutePackagesText,
        String hourPackagesText,
        String dayPackagesText,
        boolean published
) {
}
