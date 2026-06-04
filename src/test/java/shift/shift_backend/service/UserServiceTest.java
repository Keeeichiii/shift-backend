package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.Role;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.dto.user.AdminUserManagementRequest;
import shift.shift_backend.dto.user.UpdateUserRequest;
import shift.shift_backend.mapper.UserMapper;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.RoleRepository;
import shift.shift_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleWriteService userRoleWriteService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void updateChangesUsernameWhenAvailable() {
        User user = new User();
        user.setId(1L);
        user.setUsername("old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("new")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        userService.update(1L, new UpdateUserRequest("new", null, null, null, null, null, null, null, null, null, null));

        assertThat(user.getUsername()).isEqualTo("new");
        verify(userRepository).save(user);
    }

    @Test
    void updateRejectsDuplicateUsername() {
        User user = new User();
        user.setId(1L);
        user.setUsername("old");
        User existing = new User();
        existing.setId(2L);
        existing.setUsername("taken");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.update(1L, new UpdateUserRequest("taken", null, null, null, null, null, null, null, null, null, null))
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(user);
    }

    @Test
    void manageByAdminUpdatesCredentialAndRoles() {
        User user = new User();
        user.setId(1L);
        user.setUsername("old");
        Credential credential = new Credential();
        credential.setUserId(1L);
        credential.setEmail("old@mail.com");
        Role admin = new Role();
        admin.setId(2);
        admin.setName("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("new")).thenReturn(Optional.empty());
        when(credentialRepository.findByUserId(1L)).thenReturn(Optional.of(credential));
        when(credentialRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret12")).thenReturn("hash");
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.save(user)).thenReturn(user);

        userService.manageByAdmin(1L, new AdminUserManagementRequest(
                "new",
                "new@mail.com",
                "secret12",
                List.of("ADMIN"),
                null, null, null, null, null, null, null, null, null, null
        ));

        assertThat(user.getUsername()).isEqualTo("new");
        assertThat(credential.getEmail()).isEqualTo("new@mail.com");
        assertThat(credential.getPasswordHash()).isEqualTo("hash");
        verify(credentialRepository).save(credential);
        verify(userRoleWriteService).replaceRoles(1L, List.of(2L));
    }
}
