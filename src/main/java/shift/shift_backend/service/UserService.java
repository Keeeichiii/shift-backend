package shift.shift_backend.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.dto.user.AdminLicenseUpdateRequest;
import shift.shift_backend.dto.user.CreateUserRequest;
import shift.shift_backend.dto.user.UpdateUserRequest;
import shift.shift_backend.dto.user.UserDto;
import shift.shift_backend.mapper.UserMapper;
import shift.shift_backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
        user.setAvatarUrl(request.avatarUrl());
        user.setBio(request.bio());
        user.setPhone(request.phone());

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        User user = getUserEntity(id);

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
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
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
    public void delete(Long id) {
        User user = getUserEntity(id);
        userRepository.delete(user);
    }

    private User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }
}
