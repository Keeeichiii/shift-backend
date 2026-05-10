package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import shift.shift_backend.dto.auth.AuthUserResponse;
import shift.shift_backend.dto.auth.RegisterRequest;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.RoleRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private CredentialRepository credentialRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private UserRoleRepository userRoleRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private UserRoleWriteService userRoleWriteService;

	@InjectMocks
	private AuthService authService;

	@Test
	void registerThrowsConflictWhenEmailExists() {
		when(credentialRepository.findByEmail("taken@mail.com")).thenReturn(Optional.of(new Credential()));

		RegisterRequest req = new RegisterRequest("u", "taken@mail.com", "secret12", "A", "B");
		ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () -> authService.register(req));
		assertThat(ex1.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		verify(userRepository, never()).save(any());
	}

	@Test
	void registerThrowsConflictWhenUsernameExists() {
		when(credentialRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());
		when(userRepository.findByUsername("dupuser")).thenReturn(Optional.of(new User()));

		RegisterRequest req = new RegisterRequest("dupuser", "new@mail.com", "secret12", "A", "B");
		ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () -> authService.register(req));
		assertThat(ex2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void getByEmailReturnsResponse() {
		Credential c = new Credential();
		c.setUserId(5L);
		c.setEmail("a@a.com");
		User user = new User();
		user.setId(5L);
		user.setUsername("u5");

		when(credentialRepository.findByEmail("a@a.com")).thenReturn(Optional.of(c));
		when(userRepository.findById(5L)).thenReturn(Optional.of(user));
		when(userRoleRepository.findRoleNamesByUserId(5L)).thenReturn(List.of("USER"));

		AuthUserResponse r = authService.getByEmail("a@a.com");
		assertThat(r.id()).isEqualTo(5L);
		assertThat(r.email()).isEqualTo("a@a.com");
		assertThat(r.username()).isEqualTo("u5");
		assertThat(r.roles()).containsExactly("USER");
	}

	@Test
	void getByEmailThrowsWhenUnknownEmail() {
		when(credentialRepository.findByEmail(anyString())).thenReturn(Optional.empty());
		ResponseStatusException ex3 = assertThrows(ResponseStatusException.class, () -> authService.getByEmail("nope@x.com"));
		assertThat(ex3.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
