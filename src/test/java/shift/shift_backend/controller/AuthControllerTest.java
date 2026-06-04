package shift.shift_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.dto.auth.AuthUserResponse;
import shift.shift_backend.dto.auth.LoginRequest;
import shift.shift_backend.dto.auth.RegisterRequest;
import shift.shift_backend.service.AuthService;

class AuthControllerTest {

	@Test
	void registerDelegatesToService() {
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		AuthService authService = mock(AuthService.class);
		AuthController controller = new AuthController(authenticationManager, authService);

		RegisterRequest request = new RegisterRequest("u1", "a@a.com", "secret12", "F", "L");
		AuthUserResponse body = new AuthUserResponse(10L, "u1", "a@a.com", List.of("USER"), DocumentStatus.PENDING, null, null, null);
		when(authService.register(request)).thenReturn(body);

		AuthUserResponse result = controller.register(request);
		assertThat(result).isEqualTo(body);
		verify(authService).register(request);
	}

	@Test
	void loginSetsSessionContextAndReturnsUser() {
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		AuthService authService = mock(AuthService.class);
		AuthController controller = new AuthController(authenticationManager, authService);

		LoginRequest login = new LoginRequest("login@a.com", "secret12");
		var token = new UsernamePasswordAuthenticationToken(
				"login@a.com", "secret12", List.of(new SimpleGrantedAuthority("ROLE_USER")));
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(token);
		AuthUserResponse fromService = new AuthUserResponse(2L, "u2", "login@a.com", List.of("USER"), DocumentStatus.PENDING, null, null, null);
		when(authService.getByEmail("login@a.com")).thenReturn(fromService);

		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		String originalSessionId = httpRequest.getSession(true).getId();
		AuthUserResponse result = controller.login(login, httpRequest);

		assertThat(result).isEqualTo(fromService);
		HttpSession session = httpRequest.getSession(false);
		assertThat(session).isNotNull();
		assertThat(session.getId()).isNotEqualTo(originalSessionId);
		assertThat(
						session.getAttribute(
								HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
				.isNotNull();
	}
}
