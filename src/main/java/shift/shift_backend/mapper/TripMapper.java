package shift.shift_backend.mapper;

import org.springframework.stereotype.Component;
import shift.shift_backend.domain.entity.Trip;
import shift.shift_backend.dto.trip.TripDto;

@Component
public class TripMapper {

    public TripDto toDto(Trip trip) {
        return new TripDto(
                trip.getId(),
                trip.getUserId(),
                trip.getVehicleId(),
                trip.getStatus(),
                trip.getStartTime(),
                trip.getEndTime(),
                trip.getTotalMinutes(),
                trip.getParkingMinutes(),
                trip.getDistanceMeters(),
                trip.getStartLocation(),
                trip.getEndLocation()
        );
    }
}
