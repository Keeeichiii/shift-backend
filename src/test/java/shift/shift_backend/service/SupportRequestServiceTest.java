package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.SupportRequest;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.dto.support.CreateSupportRequest;
import shift.shift_backend.dto.support.PanelSupportRequestDto;
import shift.shift_backend.dto.support.SupportRequestDto;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.SupportRequestRepository;
import shift.shift_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

    @Mock
    private SupportRequestRepository supportRequestRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private SupportRequestService supportRequestService;

    @Test
    void createTrimsIncomingFields() {
        User user = new User();
        user.setId(3L);

        SupportRequest saved = new SupportRequest();
        saved.setId(11L);
        saved.setUserId(3L);
        saved.setContactChannel("telegram");
        saved.setContactValue("@user1");
        saved.setSubject("Оплата");
        saved.setMessage("Тест");
        saved.setCreatedAt(OffsetDateTime.now());

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
        when(supportRequestRepository.save(org.mockito.ArgumentMatchers.any(SupportRequest.class))).thenReturn(saved);

        SupportRequestDto dto = supportRequestService.create(authentication, new CreateSupportRequest(" telegram ", " @user1 ", " Оплата ", " Тест "));
        assertThat(dto.userId()).isEqualTo(3L);
        assertThat(dto.contactChannel()).isEqualTo("telegram");
        assertThat(dto.contactValue()).isEqualTo("@user1");
        assertThat(dto.subject()).isEqualTo("Оплата");
    }

    @Test
    void createRejectsInvalidPhoneContactValue() {
        User user = new User();
        user.setId(3L);
        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);

        assertThatThrownBy(() -> supportRequestService.create(
                authentication,
                new CreateSupportRequest("phone", "abc123", "Оплата", "Тест")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("телефона и WhatsApp");
    }

    @Test
    void createRejectsUnsupportedChannel() {
        User user = new User();
        user.setId(3L);
        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);

        assertThatThrownBy(() -> supportRequestService.create(
                authentication,
                new CreateSupportRequest("viber", "+375291234567", "Оплата", "Тест")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("корректный канал");
    }

    @Test
    void findAllForPanelsBuildsFallbackNameForMissingUser() {
        SupportRequest request = new SupportRequest();
        request.setId(20L);
        request.setUserId(200L);
        request.setContactChannel("email");
        request.setContactValue("u@x.com");
        request.setSubject("S");
        request.setMessage("M");
        request.setCreatedAt(OffsetDateTime.now());

        Credential credential = new Credential();
        credential.setUserId(200L);
        credential.setEmail("u@x.com");

        when(userRepository.findAll()).thenReturn(List.of());
        when(credentialRepository.findAll()).thenReturn(List.of(credential));
        when(supportRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(request));

        List<PanelSupportRequestDto> result = supportRequestService.findAllForPanels();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Неизвестный пользователь");
        assertThat(result.get(0).email()).isEqualTo("u@x.com");
    }

    @Test
    void deleteByIdRemovesExistingMessage() {
        when(supportRequestRepository.existsById(20L)).thenReturn(true);
        doNothing().when(supportRequestRepository).deleteById(20L);

        supportRequestService.deleteById(20L);

        verify(supportRequestRepository).deleteById(20L);
    }

    @Test
    void deleteByIdRejectsMissingMessage() {
        when(supportRequestRepository.existsById(20L)).thenReturn(false);

        assertThatThrownBy(() -> supportRequestService.deleteById(20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Сообщение не найдено");
    }
}

