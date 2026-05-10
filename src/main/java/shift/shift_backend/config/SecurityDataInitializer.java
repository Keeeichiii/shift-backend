package shift.shift_backend.config;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.Role;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.RoleRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.service.UserRoleWriteService;

@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final UserRoleWriteService userRoleWriteService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        ensureRole("USER", "Базовый пользователь");
        ensureRole("MODERATOR", "Модератор контента");
        ensureRole("ADMIN", "Администратор системы");

        ensureDefaultUser(
                "admin",
                "admin@shift.local",
                "Admin123!",
                "ADMIN",
                DocumentStatus.VERIFIED,
                "DL-ADMIN-001",
                "Системный",
                "Администратор"
        );
        ensureDefaultUser(
                "moderator",
                "moderator@shift.local",
                "Moderator123!",
                "MODERATOR",
                DocumentStatus.VERIFIED,
                "DL-MOD-001",
                "Контент",
                "Модератор"
        );
        ensureDefaultUser(
                "user",
                "user@shift.local",
                "User12345!",
                "USER",
                DocumentStatus.PENDING,
                "DL-USER-001",
                "Обычный",
                "Пользователь"
        );
    }

    private void ensureRole(String name, String description) {
        roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void ensureDefaultUser(
            String username,
            String email,
            String rawPassword,
            String roleName,
            DocumentStatus docStatus,
            String driverLicense,
            String firstName,
            String lastName
    ) {
        Credential existingCredential = credentialRepository.findByEmail(email).orElse(null);
        User user;

        if (existingCredential == null) {
            user = new User();
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setDriverLicense(driverLicense);
            user.setLicenseExpiresAt(LocalDate.now().plusYears(5));
            user.setDocStatus(docStatus);
            user = userRepository.save(user);

            Credential credential = new Credential();
            credential.setUserId(user.getId());
            credential.setEmail(email);
            credential.setPasswordHash(passwordEncoder.encode(rawPassword));
            credentialRepository.save(credential);
        } else {
            user = userRepository.findById(existingCredential.getUserId()).orElse(null);
            if (user == null) {
                return;
            }
            user.setDriverLicense(driverLicense);
            user.setLicenseExpiresAt(LocalDate.now().plusYears(5));
            user.setDocStatus(docStatus);
            userRepository.save(user);
        }

        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role != null) {
            userRoleWriteService.assignRole(user.getId(), role.getId().longValue());
        }
    }
}
