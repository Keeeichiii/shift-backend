package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Vehicle;
import shift.shift_backend.domain.enums.VehicleStatus;
import shift.shift_backend.dto.vehicle.CreateVehicleRequest;
import shift.shift_backend.dto.vehicle.UpdateVehicleRequest;
import shift.shift_backend.dto.vehicle.VehicleDto;
import shift.shift_backend.mapper.VehicleMapper;
import shift.shift_backend.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void createUsesZeroParkingRateWhenMissing() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                1, 2, "12345678901234567", "A123BC-7", "TEL-1",
                LocalDate.of(2021, 1, 1), new BigDecimal("0.40"), null, "desc"
        );
        VehicleDto dto = new VehicleDto(1L, 1, 2, "12345678901234567", "A123BC-7", "TEL-1",
                LocalDate.of(2021, 1, 1), new BigDecimal("0.40"), BigDecimal.ZERO,
                "desc", null, null, null, null);

        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleMapper.toDto(any(Vehicle.class))).thenReturn(dto);

        VehicleDto result = vehicleService.create(request);
        assertThat(result.parkingRatePerMin()).isEqualByComparingTo("0");
    }

    @Test
    void updateChangesOnlyProvidedFields() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setFleetPartnerId(10);
        vehicle.setDescription("old");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setFuelOrBatteryLevel((short) 15);

        UpdateVehicleRequest request = new UpdateVehicleRequest(
                null, new BigDecimal("0.70"), null, null, (short) 70, VehicleStatus.IN_USE, "loc"
        );

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toDto(vehicle)).thenReturn(new VehicleDto(
                5L, null, 10, null, null, null, null, new BigDecimal("0.70"), null,
                "old", null, VehicleStatus.IN_USE, (short) 70, "loc"
        ));

        VehicleDto result = vehicleService.update(5L, request);
        assertThat(result.status()).isEqualTo(VehicleStatus.IN_USE);
        assertThat(result.fuelOrBatteryLevel()).isEqualTo((short) 70);
    }

    @Test
    void deleteThrowsNotFoundForUnknownVehicle() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> vehicleService.delete(999L));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}

