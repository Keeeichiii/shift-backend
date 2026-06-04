package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.VehicleCard;
import shift.shift_backend.dto.vehiclecard.CreateVehicleCardRequest;
import shift.shift_backend.dto.vehiclecard.UpdateVehicleCardRequest;
import shift.shift_backend.repository.VehicleCardRepository;

@ExtendWith(MockitoExtension.class)
class VehicleCardServiceTest {

    @Mock
    private VehicleCardRepository vehicleCardRepository;

    @InjectMocks
    private VehicleCardService vehicleCardService;

    @Test
    void createTrimsFieldsAndConvertsBlankToNull() {
        CreateVehicleCardRequest request = new CreateVehicleCardRequest(
                "  Toyota RAV4  ",
                "  toyota-rav4  ",
                "  crossover  ",
                false,
                "  /images/cars/rav4.png  ",
                new BigDecimal("0.99"),
                " ",
                " short ",
                " details ",
                "  Автомат ",
                "  Бензин ",
                " 2.0L ",
                " cond ",
                " feat ",
                " min ",
                " hour ",
                " day ",
                true
        );

        VehicleCard saved = new VehicleCard();
        saved.setId(10L);
        saved.setTitle("Toyota RAV4");
        saved.setSlug("toyota-rav4");
        saved.setCategory("crossover");
        saved.setImagePath("/images/cars/rav4.png");
        saved.setPricePerMinute(new BigDecimal("0.99"));
        saved.setPublished(true);

        when(vehicleCardRepository.findBySlug("toyota-rav4")).thenReturn(Optional.empty());
        when(vehicleCardRepository.save(any(VehicleCard.class))).thenReturn(saved);

        var result = vehicleCardService.create(request);

        ArgumentCaptor<VehicleCard> captor = ArgumentCaptor.forClass(VehicleCard.class);
        verify(vehicleCardRepository).save(captor.capture());
        VehicleCard entity = captor.getValue();
        assertThat(entity.getTitle()).isEqualTo("Toyota RAV4");
        assertThat(entity.getSlug()).isEqualTo("toyota-rav4");
        assertThat(entity.getCategory()).isEqualTo("crossover");
        assertThat(entity.getImagePath()).isEqualTo("/images/cars/rav4.png");
        assertThat(entity.getBadge()).isNull();
        assertThat(entity.getShortDescription()).isEqualTo("short");
        assertThat(result.id()).isEqualTo(10L);
    }

    @Test
    void createChecksConflictUsingTrimmedSlug() {
        CreateVehicleCardRequest request = new CreateVehicleCardRequest(
                "Toyota RAV4",
                "  toyota-rav4  ",
                "crossover",
                false,
                "/images/cars/rav4.png",
                new BigDecimal("0.99"),
                null, null, null, null, null, null, null, null, null, null, null,
                true
        );
        VehicleCard existing = new VehicleCard();
        existing.setId(10L);

        when(vehicleCardRepository.findBySlug("toyota-rav4")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> vehicleCardService.create(request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(vehicleCardRepository, never()).save(any());
    }

    @Test
    void updateThrowsConflictWhenSlugBelongsToAnotherCard() {
        VehicleCard current = new VehicleCard();
        current.setId(1L);
        VehicleCard other = new VehicleCard();
        other.setId(2L);
        UpdateVehicleCardRequest request = new UpdateVehicleCardRequest(
                "t", "dup", "standard", true, "/img.png", new BigDecimal("1.0"),
                null, null, null, null, null, null, null, null, null, null, null, true
        );

        when(vehicleCardRepository.findById(1L)).thenReturn(Optional.of(current));
        when(vehicleCardRepository.findBySlug("dup")).thenReturn(Optional.of(other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> vehicleCardService.update(1L, request));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(vehicleCardRepository, never()).save(any());
    }
}

