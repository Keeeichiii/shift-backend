package shift.shift_backend.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import shift.shift_backend.dto.longbooking.LongBookingBusyIntervalDto;
import shift.shift_backend.dto.vehiclecard.CreateVehicleCardRequest;
import shift.shift_backend.dto.vehiclecard.UpdateVehicleCardRequest;
import shift.shift_backend.dto.vehiclecard.VehicleCardDto;
import shift.shift_backend.dto.vehiclecard.VehicleCardImageUploadResponse;
import shift.shift_backend.service.LongBookingOrderService;
import shift.shift_backend.service.VehicleCardImageUploadService;
import shift.shift_backend.service.VehicleCardService;

@RestController
@RequestMapping("/api/vehicle-cards")
@RequiredArgsConstructor
public class VehicleCardController {

    private final VehicleCardService vehicleCardService;
    private final VehicleCardImageUploadService vehicleCardImageUploadService;
    private final LongBookingOrderService longBookingOrderService;

    @GetMapping("/public")
    public List<VehicleCardDto> getPublishedCards() {
        return vehicleCardService.getPublishedCards();
    }

    @GetMapping("/public/{slug}")
    public VehicleCardDto getPublicCard(@PathVariable String slug) {
        return vehicleCardService.getBySlug(slug);
    }

    @GetMapping("/public/{slug}/long-booking-busy-intervals")
    public List<LongBookingBusyIntervalDto> getLongBookingBusyIntervals(
            @PathVariable String slug,
            @RequestParam("from") OffsetDateTime from,
            @RequestParam("to") OffsetDateTime to
    ) {
        return longBookingOrderService.listBusyIntervalsForPublishedCard(slug, from, to);
    }

    @GetMapping
    public List<VehicleCardDto> getAllCards() {
        return vehicleCardService.getAllCards();
    }

	@PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public VehicleCardImageUploadResponse uploadImage(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "category", required = false) String category
	) {
		return new VehicleCardImageUploadResponse(vehicleCardImageUploadService.store(file, category));
	}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleCardDto create(@Valid @RequestBody CreateVehicleCardRequest request) {
        return vehicleCardService.create(request);
    }

    @PutMapping("/{id}")
    public VehicleCardDto update(@PathVariable Long id, @Valid @RequestBody UpdateVehicleCardRequest request) {
        return vehicleCardService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        vehicleCardService.delete(id);
    }
}
