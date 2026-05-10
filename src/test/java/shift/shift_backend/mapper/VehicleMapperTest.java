package shift.shift_backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import shift.shift_backend.domain.entity.Vehicle;
import shift.shift_backend.domain.enums.VehicleStatus;
import shift.shift_backend.dto.vehicle.VehicleDto;

class VehicleMapperTest {

	private final VehicleMapper mapper = new VehicleMapper();

	@Test
	void mapsVehicleToDto() {
		Vehicle v = new Vehicle();
		v.setId(9L);
		v.setBrandId(1);
		v.setFleetPartnerId(2);
		v.setVin("VIN1");
		v.setLicensePlate("A123");
		v.setTelematicsDeviceId("TEL-1");
		v.setReleaseDate(LocalDate.of(2020, 1, 2));
		v.setBaseRatePerMin(new BigDecimal("1.50"));
		v.setParkingRatePerMin(new BigDecimal("0.10"));
		v.setDescription("d");
		v.setAverageRating(new BigDecimal("4.2"));
		v.setStatus(VehicleStatus.AVAILABLE);
		v.setFuelOrBatteryLevel((short) 80);
		v.setCurrentLocation(null);

		VehicleDto dto = mapper.toDto(v);
		assertThat(dto.id()).isEqualTo(9L);
		assertThat(dto.brandId()).isEqualTo(1);
		assertThat(dto.fleetPartnerId()).isEqualTo(2);
		assertThat(dto.vin()).isEqualTo("VIN1");
		assertThat(dto.licensePlate()).isEqualTo("A123");
		assertThat(dto.status()).isEqualTo(VehicleStatus.AVAILABLE);
		assertThat(dto.fuelOrBatteryLevel()).isEqualTo((short) 80);
	}
}
