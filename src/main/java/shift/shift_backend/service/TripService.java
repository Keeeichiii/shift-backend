package shift.shift_backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Trip;
import shift.shift_backend.domain.entity.Vehicle;
import shift.shift_backend.domain.enums.TripStatus;
import shift.shift_backend.domain.enums.VehicleStatus;
import shift.shift_backend.dto.trip.CreateTripRequest;
import shift.shift_backend.dto.trip.TripDto;
import shift.shift_backend.dto.trip.UpdateTripRequest;
import shift.shift_backend.mapper.TripMapper;
import shift.shift_backend.repository.TripRepository;
import shift.shift_backend.repository.VehicleRepository;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<TripDto> getAll(Authentication authentication) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        return tripRepository.findAllByUserId(userId).stream().map(tripMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TripDto> getCurrentUserTrips(Authentication authentication) {
        return getAll(authentication);
    }

    @Transactional(readOnly = true)
    public TripDto getById(Authentication authentication, Long id) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        return tripMapper.toDto(getOwnedTripEntity(userId, id));
    }

    @Transactional
    public TripDto create(Authentication authentication, CreateTripRequest request) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        Vehicle vehicle = getVehicleEntity(request.vehicleId());
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle is not available for booking");
        }

        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setVehicleId(request.vehicleId());
        trip.setStatus(TripStatus.RESERVED);
        trip.setStartLocation(request.startLocation());
        vehicle.setStatus(VehicleStatus.BOOKED);

        return tripMapper.toDto(tripRepository.save(trip));
    }

    @Transactional
    public TripDto update(Authentication authentication, Long id, UpdateTripRequest request) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        Trip trip = getOwnedTripEntity(userId, id);
        Vehicle vehicle = getVehicleEntity(trip.getVehicleId());

        if (request.status() != null) {
            trip.setStatus(request.status());
            if (request.status() == TripStatus.ACTIVE && trip.getStartTime() == null) {
                trip.setStartTime(OffsetDateTime.now());
                vehicle.setStatus(VehicleStatus.IN_USE);
            }
            if (request.status() == TripStatus.COMPLETED && trip.getEndTime() == null) {
                trip.setEndTime(OffsetDateTime.now());
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }
            if (request.status() == TripStatus.CANCELED) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }
            if (request.status() == TripStatus.PAUSED) {
                vehicle.setStatus(VehicleStatus.BOOKED);
            }
        }
        if (request.startTime() != null) {
            trip.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            trip.setEndTime(request.endTime());
        }
        if (request.totalMinutes() != null) {
            trip.setTotalMinutes(request.totalMinutes());
        }
        if (request.parkingMinutes() != null) {
            trip.setParkingMinutes(request.parkingMinutes());
        }
        if (request.distanceMeters() != null) {
            trip.setDistanceMeters(request.distanceMeters());
        }
        if (request.endLocation() != null) {
            trip.setEndLocation(request.endLocation());
        }

        return tripMapper.toDto(tripRepository.save(trip));
    }

    @Transactional
    public void delete(Authentication authentication, Long id) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        Trip trip = getOwnedTripEntity(userId, id);
        Vehicle vehicle = getVehicleEntity(trip.getVehicleId());
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        tripRepository.delete(trip);
    }

    private Trip getOwnedTripEntity(Long userId, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId));
        if (!trip.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Trip does not belong to current user");
        }
        return trip;
    }

    private Vehicle getVehicleEntity(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found: " + id));
    }
}
