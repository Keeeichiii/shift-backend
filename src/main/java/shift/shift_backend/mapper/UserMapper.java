package shift.shift_backend.mapper;

import org.springframework.stereotype.Component;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.dto.user.UserDto;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getRegionId(),
                user.getUsername(),
                user.getRegistrationDate(),
                user.getFirstName(),
                user.getLastName(),
                user.getPersonalIdNumber(),
                user.getDriverLicense(),
                user.getLicenseExpiresAt(),
                user.getDrivingBanUntil(),
                user.getDocStatus(),
                user.getProfileName(),
                user.getBio(),
                user.getLastActivity(),
                user.getPhone()
        );
    }
}
