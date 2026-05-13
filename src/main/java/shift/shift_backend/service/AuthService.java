package shift.shift_backend.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.Role;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.dto.auth.AuthUserResponse;
import shift.shift_backend.dto.auth.RegisterRequest;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.RoleRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.UserRoleRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleWriteService userRoleWriteService;

    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        if (credentialRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setDocStatus(DocumentStatus.PENDING);
        User savedUser = userRepository.save(user);

        Credential credential = new Credential();
        credential.setUserId(savedUser.getId());
        credential.setEmail(request.email());
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credentialRepository.save(credential);

        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        userRoleWriteService.assignRole(savedUser.getId(), role.getId().longValue());

        return buildResponse(savedUser, credential.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getByEmail(String email) {
        Credential credential = credentialRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));
        User user = userRepository.findById(credential.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return buildResponse(user, credential.getEmail());
    }

    private AuthUserResponse buildResponse(User user, String email) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                email,
                roles,
                user.getDocStatus(),
                user.getLicenseExpiresAt(),
                user.getDrivingBanUntil(),
                user.getDriverLicense()
        );
    }
}
