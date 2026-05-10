package shift.shift_backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import shift.shift_backend.domain.entity.Trip;
import shift.shift_backend.domain.enums.TripStatus;
import shift.shift_backend.dto.trip.TripDto;

class TripMapperTest {

	private final TripMapper mapper = new TripMapper();

	@Test
	void mapsTripToDto() {
		Trip trip = new Trip();
		trip.setId(1L);
		trip.setUserId(2L);
		trip.setVehicleId(3L);
		trip.setStatus(TripStatus.RESERVED);

		TripDto dto = mapper.toDto(trip);
		assertThat(dto.id()).isEqualTo(1L);
		assertThat(dto.userId()).isEqualTo(2L);
		assertThat(dto.vehicleId()).isEqualTo(3L);
		assertThat(dto.status()).isEqualTo(TripStatus.RESERVED);
	}
}
