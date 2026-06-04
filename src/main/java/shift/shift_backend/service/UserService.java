package shift.shift_backend.service;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.Role;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.dto.user.AdminUserManagementRequest;
import shift.shift_backend.dto.user.AdminLicenseUpdateRequest;
import shift.shift_backend.dto.user.CreateUserRequest;
import shift.shift_backend.dto.user.UpdateUserRequest;
import shift.shift_backend.dto.user.UserDto;
import shift.shift_backend.mapper.UserMapper;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.RoleRepository;
import shift.shift_backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CredentialRepository credentialRepository;
    private final RoleRepository roleRepository;
    private final UserRoleWriteService userRoleWriteService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getAll() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        return userMapper.toDto(getUserEntity(id));
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        User user = new User();
        user.setRegionId(request.regionId());
        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPersonalIdNumber(request.personalIdNumber());
        user.setDriverLicense(request.driverLicense());
        user.setLicenseExpiresAt(request.licenseExpiresAt());
        user.setProfileName(request.profileName());
        user.setBio(request.bio());
        user.setPhone(request.phone());

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        User user = getUserEntity(id);

        updateUsername(user, request.username());
        if (request.regionId() != null) {
            user.setRegionId(request.regionId());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
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
        if (request.driverLicense() != null) {
            user.setDriverLicense(request.driverLicense());
        }
        if (request.licenseExpiresAt() != null) {
            user.setLicenseExpiresAt(request.licenseExpiresAt());
        }
        if (request.drivingBanUntil() != null) {
            user.setDrivingBanUntil(request.drivingBanUntil());
        }
        if (request.docStatus() != null) {
            user.setDocStatus(request.docStatus());
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateLicenseData(Long id, AdminLicenseUpdateRequest request) {
        User user = getUserEntity(id);
        user.setDriverLicense(request.driverLicense());
        user.setLicenseExpiresAt(request.licenseExpiresAt());
        user.setDrivingBanUntil(request.drivingBanUntil());
        if (request.docStatus() != null) {
            user.setDocStatus(request.docStatus());
        }
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto manageByAdmin(Long id, AdminUserManagementRequest request) {
        User user = getUserEntity(id);
        applyProfileUpdates(user, request);
        updateCredential(id, request);
        updateRoles(id, request.roles());
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = getUserEntity(id);
        userRepository.delete(user);
    }

    private User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private void applyProfileUpdates(User user, AdminUserManagementRequest request) {
        updateUsername(user, request.username());
        if (request.regionId() != null) {
            user.setRegionId(request.regionId());
        }
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
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
        if (request.driverLicense() != null) {
            user.setDriverLicense(request.driverLicense());
        }
        if (request.licenseExpiresAt() != null) {
            user.setLicenseExpiresAt(request.licenseExpiresAt());
        }
        if (request.drivingBanUntil() != null) {
            user.setDrivingBanUntil(request.drivingBanUntil());
        }
        if (request.docStatus() != null) {
            user.setDocStatus(request.docStatus());
        }
    }

    private void updateUsername(User user, String username) {
        if (username != null && !username.equals(user.getUsername())) {
            userRepository.findByUsername(username).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
                }
            });
            user.setUsername(username);
        }
    }

    private void updateCredential(Long userId, AdminUserManagementRequest request) {
        boolean emailProvided = request.email() != null && !request.email().isBlank();
        boolean passwordProvided = request.password() != null && !request.password().isBlank();
        if (!emailProvided && !passwordProvided) {
            return;
        }

        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));
        if (emailProvided && !request.email().equals(credential.getEmail())) {
            credentialRepository.findByEmail(request.email()).ifPresent(existing -> {
                if (!existing.getUserId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                }
            });
            credential.setEmail(request.email());
        }
        if (passwordProvided) {
            credential.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        credentialRepository.save(credential);
    }

    private void updateRoles(Long userId, List<String> requestedRoles) {
        if (requestedRoles == null) {
            return;
        }
        List<Long> roleIds = requestedRoles.stream()
                .map(role -> role == null ? "" : role.trim().toUpperCase(Locale.ROOT))
                .filter(role -> !role.isBlank())
                .distinct()
                .map(role -> roleRepository.findByName(role)
                        .map(Role::getId)
                        .map(Integer::longValue)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + role)))
                .toList();
        if (roleIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must have at least one role");
        }
        userRoleWriteService.replaceRoles(userId, roleIds);
    }
}
