package shift.shift_backend.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import shift.shift_backend.dto.longbooking.CreateLongBookingOrderRequest;
import shift.shift_backend.dto.longbooking.LongBookingOrderDto;
import shift.shift_backend.dto.trip.TripDto;
import shift.shift_backend.dto.user.LicenseSubmissionRequest;
import shift.shift_backend.dto.user.MeProfileDto;
import shift.shift_backend.dto.user.UpdateMeRequest;
import shift.shift_backend.service.CurrentUserService;
import shift.shift_backend.service.LongBookingOrderService;
import shift.shift_backend.service.TripService;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final TripService tripService;
    private final LongBookingOrderService longBookingOrderService;

    @GetMapping
    public MeProfileDto getProfile(Authentication authentication) {
        return currentUserService.getCurrentProfile(authentication);
    }

    @PutMapping
    public MeProfileDto updateProfile(Authentication authentication, @Valid @RequestBody UpdateMeRequest request) {
        return currentUserService.updateCurrentProfile(authentication, request);
    }

    @PutMapping("/license-submission")
    public MeProfileDto submitLicense(
            Authentication authentication,
            @Valid @RequestBody LicenseSubmissionRequest request
    ) {
        return currentUserService.submitLicenseForModeration(authentication, request);
    }

    @GetMapping("/long-booking-orders")
    public List<LongBookingOrderDto> getLongBookingOrders(Authentication authentication) {
        return longBookingOrderService.listForCurrentUser(authentication);
    }

    @PostMapping("/long-booking-orders")
    @ResponseStatus(HttpStatus.CREATED)
    public LongBookingOrderDto createLongBookingOrder(
            Authentication authentication,
            @Valid @RequestBody CreateLongBookingOrderRequest request
    ) {
        return longBookingOrderService.create(authentication, request);
    }

    @GetMapping("/trips")
    public List<TripDto> getCurrentUserTrips(Authentication authentication) {
        return tripService.getCurrentUserTrips(authentication);
    }
}
