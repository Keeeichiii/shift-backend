package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.dto.user.UpdateMeRequest;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private CurrentUserService currentUserService;

    @Test
    void getCurrentCredentialRejectsMissingAuthentication() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> currentUserService.getCurrentCredential(null)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateCurrentProfileUpdatesAllowedFieldsAndActivity() {
        Credential credential = credential(5L, "user@mail.com");
        User user = user(5L, "old", "Old", "Name");

        when(authentication.getName()).thenReturn("user@mail.com");
        when(credentialRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(credential));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRoleRepository.findRoleNamesByUserId(5L)).thenReturn(List.of("USER"));

        var result = currentUserService.updateCurrentProfile(
                authentication,
                new UpdateMeRequest(null, "New", "User", "12345678901234", "profile", "bio", "+375")
        );

        assertThat(result.firstName()).isEqualTo("New");
        assertThat(result.lastName()).isEqualTo("User");
        assertThat(result.personalIdNumber()).isEqualTo("12345678901234");
        assertThat(result.profileName()).isEqualTo("profile");
        assertThat(result.bio()).isEqualTo("bio");
        assertThat(result.phone()).isEqualTo("+375");
        assertThat(result.lastActivity()).isNotNull();
    }

    private static Credential credential(Long userId, String email) {
        Credential credential = new Credential();
        credential.setUserId(userId);
        credential.setEmail(email);
        return credential;
    }

    private static User user(Long id, String username, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}
