package shift.shift_backend.dto.vehiclecard;

import java.math.BigDecimal;

public record VehicleCardDto(
        Long id,
        String title,
        String slug,
        String category,
        boolean wrapped,
        String imagePath,
        BigDecimal pricePerMinute,
        String badge,
        String shortDescription,
        String detailDescription,
        String transmission,
        String fuelType,
        String engine,
        String conditionsText,
        String featuresText,
        String minutePackagesText,
        String hourPackagesText,
        String dayPackagesText,
        boolean published
) {
}
