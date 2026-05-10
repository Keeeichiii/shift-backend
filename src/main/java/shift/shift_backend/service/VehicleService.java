package shift.shift_backend.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Vehicle;
import shift.shift_backend.dto.vehicle.CreateVehicleRequest;
import shift.shift_backend.dto.vehicle.UpdateVehicleRequest;
import shift.shift_backend.dto.vehicle.VehicleDto;
import shift.shift_backend.mapper.VehicleMapper;
import shift.shift_backend.repository.VehicleRepository;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional(readOnly = true)
    public List<VehicleDto> getAll() {
        return vehicleRepository.findAll().stream().map(vehicleMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VehicleDto getById(Long id) {
        return vehicleMapper.toDto(getVehicleEntity(id));
    }

    @Transactional
    public VehicleDto create(CreateVehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setBrandId(request.brandId());
        vehicle.setFleetPartnerId(request.fleetPartnerId());
        vehicle.setVin(request.vin());
        vehicle.setLicensePlate(request.licensePlate());
        vehicle.setTelematicsDeviceId(request.telematicsDeviceId());
        vehicle.setReleaseDate(request.releaseDate());
        vehicle.setBaseRatePerMin(request.baseRatePerMin());
        vehicle.setParkingRatePerMin(
                request.parkingRatePerMin() == null ? BigDecimal.ZERO : request.parkingRatePerMin()
        );
        vehicle.setDescription(request.description());

        return vehicleMapper.toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleDto update(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = getVehicleEntity(id);

        if (request.fleetPartnerId() != null) {
            vehicle.setFleetPartnerId(request.fleetPartnerId());
        }
        if (request.baseRatePerMin() != null) {
            vehicle.setBaseRatePerMin(request.baseRatePerMin());
        }
        if (request.parkingRatePerMin() != null) {
            vehicle.setParkingRatePerMin(request.parkingRatePerMin());
        }
        if (request.description() != null) {
            vehicle.setDescription(request.description());
        }
        if (request.fuelOrBatteryLevel() != null) {
            vehicle.setFuelOrBatteryLevel(request.fuelOrBatteryLevel());
        }
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }
        if (request.currentLocation() != null) {
            vehicle.setCurrentLocation(request.currentLocation());
        }

        return vehicleMapper.toDto(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = getVehicleEntity(id);
        vehicleRepository.delete(vehicle);
    }

    private Vehicle getVehicleEntity(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found: " + id));
    }
}
