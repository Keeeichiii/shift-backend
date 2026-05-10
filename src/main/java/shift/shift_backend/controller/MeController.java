package shift.shift_backend.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shift.shift_backend.dto.trip.TripDto;
import shift.shift_backend.dto.user.LicenseSubmissionRequest;
import shift.shift_backend.dto.user.MeProfileDto;
import shift.shift_backend.dto.user.UpdateMeRequest;
import shift.shift_backend.service.CurrentUserService;
import shift.shift_backend.service.TripService;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final TripService tripService;

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

    @GetMapping("/trips")
    public List<TripDto> getCurrentUserTrips(Authentication authentication) {
        return tripService.getCurrentUserTrips(authentication);
    }
}
