package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripMapper tripMapper;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private TripService tripService;

    @Test
    void createReservesTripAndBooksVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(4L);
        vehicle.setStatus(VehicleStatus.AVAILABLE);

        Trip saved = new Trip();
        saved.setId(1L);
        saved.setUserId(7L);
        saved.setVehicleId(4L);
        saved.setStatus(TripStatus.RESERVED);

        when(currentUserService.getCurrentUserId(authentication)).thenReturn(7L);
        when(vehicleRepository.findById(4L)).thenReturn(Optional.of(vehicle));
        when(tripRepository.save(any(Trip.class))).thenReturn(saved);
        when(tripMapper.toDto(saved)).thenReturn(new TripDto(1L, 7L, 4L, TripStatus.RESERVED, null, null, null, null, null, null, null));

        TripDto dto = tripService.create(authentication, new CreateTripRequest(4L, "A"));
        assertThat(dto.status()).isEqualTo(TripStatus.RESERVED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.BOOKED);
    }

    @Test
    void createThrowsConflictWhenVehicleUnavailable() {
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.IN_USE);
        when(currentUserService.getCurrentUserId(authentication)).thenReturn(7L);
        when(vehicleRepository.findById(4L)).thenReturn(Optional.of(vehicle));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> tripService.create(authentication, new CreateTripRequest(4L, "A"))
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateCompletedSetsEndTimeAndVehicleAvailable() {
        Trip trip = new Trip();
        trip.setId(2L);
        trip.setUserId(7L);
        trip.setVehicleId(9L);
        trip.setStatus(TripStatus.ACTIVE);
        trip.setStartTime(OffsetDateTime.now().minusMinutes(20));

        Vehicle vehicle = new Vehicle();
        vehicle.setId(9L);
        vehicle.setStatus(VehicleStatus.IN_USE);

        when(currentUserService.getCurrentUserId(authentication)).thenReturn(7L);
        when(tripRepository.findById(2L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(9L)).thenReturn(Optional.of(vehicle));
        when(tripRepository.save(trip)).thenReturn(trip);
        when(tripMapper.toDto(trip)).thenReturn(new TripDto(2L, 7L, 9L, TripStatus.COMPLETED,
                trip.getStartTime(), OffsetDateTime.now(), null, null, null, null, "B"));

        TripDto dto = tripService.update(authentication, 2L, new UpdateTripRequest(TripStatus.COMPLETED, null, null, null, null, null, "B"));
        assertThat(dto.status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.getEndTime()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }
}

