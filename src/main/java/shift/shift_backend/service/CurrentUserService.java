package shift.shift_backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.dto.user.LicenseSubmissionRequest;
import shift.shift_backend.dto.user.MeProfileDto;
import shift.shift_backend.dto.user.UpdateMeRequest;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.UserRoleRepository;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public MeProfileDto getCurrentProfile(Authentication authentication) {
        Credential credential = getCurrentCredential(authentication);
        User user = getCurrentUser(authentication);
        return toMeProfileDto(user, credential);
    }

    @Transactional
    public MeProfileDto updateCurrentProfile(Authentication authentication, UpdateMeRequest request) {
        Credential credential = getCurrentCredential(authentication);
        User user = getCurrentUser(authentication);

        if (request.regionId() != null) {
            user.setRegionId(request.regionId());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.personalIdNumber() != null) {
            user.setPersonalIdNumber(request.personalIdNumber());
        }
        if (request.profileName() != null) {
            user.setProfileName(request.profileName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        user.setLastActivity(OffsetDateTime.now());
        User savedUser = userRepository.save(user);
        return toMeProfileDto(savedUser, credential);
    }

    @Transactional
    public MeProfileDto submitLicenseForModeration(
            Authentication authentication,
            @Valid LicenseSubmissionRequest request
    ) {
        Credential credential = getCurrentCredential(authentication);
        User user = getCurrentUser(authentication);

        user.setLicenseFrontImage(request.frontImageData());
        user.setLicenseBackImage(request.backImageData());
        user.setPassportMainImage(request.passportMainImageData());
        user.setLicenseSubmittedAt(OffsetDateTime.now());
        user.setDocStatus(DocumentStatus.PENDING);
        user.setLastActivity(OffsetDateTime.now());

        User savedUser = userRepository.save(user);
        return toMeProfileDto(savedUser, credential);
    }

    @Transactional(readOnly = true)
    public Long getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(Authentication authentication) {
        Credential credential = getCurrentCredential(authentication);
        return userRepository.findById(credential.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current user not found"));
    }

    @Transactional(readOnly = true)
    public Credential getCurrentCredential(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return credentialRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current credential not found"));
    }

    private MeProfileDto toMeProfileDto(User user, Credential credential) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return new MeProfileDto(
                user.getId(),
                credential.getEmail(),
                roles,
                user.getRegionId(),
                user.getUsername(),
                user.getRegistrationDate(),
                user.getFirstName(),
                user.getLastName(),
                user.getPersonalIdNumber(),
                user.getDriverLicense(),
                user.getLicenseExpiresAt(),
                user.getDrivingBanUntil(),
                user.getLicenseFrontImage(),
                user.getLicenseBackImage(),
                user.getPassportMainImage(),
                user.getLicenseSubmittedAt(),
                user.getDocStatus(),
                user.getProfileName(),
                user.getBio(),
                user.getLastActivity(),
                user.getPhone()
        );
    }
}
