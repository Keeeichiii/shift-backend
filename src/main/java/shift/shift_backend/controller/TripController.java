package shift.shift_backend.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import shift.shift_backend.dto.trip.CreateTripRequest;
import shift.shift_backend.dto.trip.TripDto;
import shift.shift_backend.dto.trip.UpdateTripRequest;
import shift.shift_backend.service.TripService;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public List<TripDto> getAll(Authentication authentication) {
        return tripService.getAll(authentication);
    }

    @GetMapping("/{id}")
    public TripDto getById(Authentication authentication, @PathVariable Long id) {
        return tripService.getById(authentication, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripDto create(Authentication authentication, @Valid @RequestBody CreateTripRequest request) {
        return tripService.create(authentication, request);
    }

    @PutMapping("/{id}")
    public TripDto update(Authentication authentication, @PathVariable Long id, @Valid @RequestBody UpdateTripRequest request) {
        return tripService.update(authentication, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        tripService.delete(authentication, id);
    }
}
