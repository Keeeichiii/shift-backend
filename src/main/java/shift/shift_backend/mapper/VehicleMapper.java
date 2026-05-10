package shift.shift_backend.mapper;

import org.springframework.stereotype.Component;
import shift.shift_backend.domain.entity.Vehicle;
import shift.shift_backend.dto.vehicle.VehicleDto;

@Component
public class VehicleMapper {

    public VehicleDto toDto(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getBrandId(),
                vehicle.getFleetPartnerId(),
                vehicle.getVin(),
                vehicle.getLicensePlate(),
                vehicle.getTelematicsDeviceId(),
                vehicle.getReleaseDate(),
                vehicle.getBaseRatePerMin(),
                vehicle.getParkingRatePerMin(),
                vehicle.getDescription(),
                vehicle.getAverageRating(),
                vehicle.getStatus(),
                vehicle.getFuelOrBatteryLevel(),
                vehicle.getCurrentLocation()
        );
    }
}
