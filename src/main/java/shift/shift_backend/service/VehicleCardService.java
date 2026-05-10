package shift.shift_backend.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.VehicleCard;
import shift.shift_backend.dto.vehiclecard.CreateVehicleCardRequest;
import shift.shift_backend.dto.vehiclecard.UpdateVehicleCardRequest;
import shift.shift_backend.dto.vehiclecard.VehicleCardDto;
import shift.shift_backend.repository.VehicleCardRepository;

@Service
@RequiredArgsConstructor
public class VehicleCardService {

    private final VehicleCardRepository vehicleCardRepository;

    @Transactional(readOnly = true)
    public List<VehicleCardDto> getPublishedCards() {
        return vehicleCardRepository.findAllByPublishedTrueOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleCardDto> getAllCards() {
        return vehicleCardRepository.findAll().stream()
                .sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleCardDto getBySlug(String slug) {
        return toDto(getEntityBySlug(slug));
    }

    @Transactional
    public VehicleCardDto create(CreateVehicleCardRequest request) {
        vehicleCardRepository.findBySlug(request.slug()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Карточка с таким slug уже существует.");
        });

        VehicleCard card = new VehicleCard();
        apply(card, request);
        return toDto(vehicleCardRepository.save(card));
    }

    @Transactional
    public VehicleCardDto update(Long id, UpdateVehicleCardRequest request) {
        VehicleCard card = getEntityById(id);
        vehicleCardRepository.findBySlug(request.slug()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Карточка с таким slug уже существует.");
            }
        });

        apply(card, request);
        return toDto(vehicleCardRepository.save(card));
    }

    @Transactional
    public void delete(Long id) {
        vehicleCardRepository.delete(getEntityById(id));
    }

    private void apply(VehicleCard card, CreateVehicleCardRequest request) {
        card.setTitle(request.title().trim());
        card.setSlug(request.slug().trim());
        card.setCategory(request.category().trim());
        card.setWrapped(request.wrapped());
        card.setImagePath(request.imagePath().trim());
        card.setPricePerMinute(request.pricePerMinute());
        card.setBadge(blankToNull(request.badge()));
        card.setShortDescription(blankToNull(request.shortDescription()));
        card.setDetailDescription(blankToNull(request.detailDescription()));
        card.setTransmission(blankToNull(request.transmission()));
        card.setFuelType(blankToNull(request.fuelType()));
        card.setEngine(blankToNull(request.engine()));
        card.setConditionsText(blankToNull(request.conditionsText()));
        card.setFeaturesText(blankToNull(request.featuresText()));
        card.setMinutePackagesText(blankToNull(request.minutePackagesText()));
        card.setHourPackagesText(blankToNull(request.hourPackagesText()));
        card.setDayPackagesText(blankToNull(request.dayPackagesText()));
        card.setPublished(request.published());
    }

    private void apply(VehicleCard card, UpdateVehicleCardRequest request) {
        card.setTitle(request.title().trim());
        card.setSlug(request.slug().trim());
        card.setCategory(request.category().trim());
        card.setWrapped(request.wrapped());
        card.setImagePath(request.imagePath().trim());
        card.setPricePerMinute(request.pricePerMinute());
        card.setBadge(blankToNull(request.badge()));
        card.setShortDescription(blankToNull(request.shortDescription()));
        card.setDetailDescription(blankToNull(request.detailDescription()));
        card.setTransmission(blankToNull(request.transmission()));
        card.setFuelType(blankToNull(request.fuelType()));
        card.setEngine(blankToNull(request.engine()));
        card.setConditionsText(blankToNull(request.conditionsText()));
        card.setFeaturesText(blankToNull(request.featuresText()));
        card.setMinutePackagesText(blankToNull(request.minutePackagesText()));
        card.setHourPackagesText(blankToNull(request.hourPackagesText()));
        card.setDayPackagesText(blankToNull(request.dayPackagesText()));
        card.setPublished(request.published());
    }

    private VehicleCard getEntityById(Long id) {
        return vehicleCardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Карточка машины не найдена."));
    }

    private VehicleCard getEntityBySlug(String slug) {
        return vehicleCardRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Карточка машины не найдена."));
    }

    private VehicleCardDto toDto(VehicleCard card) {
        return new VehicleCardDto(
                card.getId(),
                card.getTitle(),
                card.getSlug(),
                card.getCategory(),
                card.isWrapped(),
                card.getImagePath(),
                card.getPricePerMinute(),
                card.getBadge(),
                card.getShortDescription(),
                card.getDetailDescription(),
                card.getTransmission(),
                card.getFuelType(),
                card.getEngine(),
                card.getConditionsText(),
                card.getFeaturesText(),
                card.getMinutePackagesText(),
                card.getHourPackagesText(),
                card.getDayPackagesText(),
                card.isPublished()
        );
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
