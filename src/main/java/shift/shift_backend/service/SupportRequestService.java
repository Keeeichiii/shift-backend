package shift.shift_backend.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.SupportRequest;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.dto.support.CreateSupportRequest;
import shift.shift_backend.dto.support.PanelSupportRequestDto;
import shift.shift_backend.dto.support.SupportRequestDto;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.SupportRequestRepository;
import shift.shift_backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class SupportRequestService {

    private static final Set<String> ALLOWED_CHANNELS = Set.of("phone", "telegram", "whatsapp", "email");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[\\d\\s()-]{7,25}$");
    private static final Pattern TELEGRAM_PATTERN = Pattern.compile("^@?[A-Za-z0-9_]{5,32}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final SupportRequestRepository supportRequestRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    @Transactional
    public SupportRequestDto create(Authentication authentication, CreateSupportRequest request) {
        User user = currentUserService.getCurrentUser(authentication);
        String contactChannel = trim(request.contactChannel());
        String contactValue = trim(request.contactValue());
        String subject = trim(request.subject());
        String message = trim(request.message());
        validateIncomingRequest(contactChannel, contactValue, subject, message);

        SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUserId(user.getId());
        supportRequest.setContactChannel(contactChannel);
        supportRequest.setContactValue(contactValue);
        supportRequest.setSubject(subject);
        supportRequest.setMessage(message);

        SupportRequest saved = supportRequestRepository.save(supportRequest);
        return new SupportRequestDto(
                saved.getId(),
                saved.getUserId(),
                saved.getContactChannel(),
                saved.getContactValue(),
                saved.getSubject(),
                saved.getMessage(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PanelSupportRequestDto> findAllForPanels() {
        Map<Long, User> usersById = userRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, user -> user));
        Map<Long, String> emailsByUserId = credentialRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        credential -> credential.getUserId(),
                        credential -> credential.getEmail(),
                        (left, right) -> left
                ));

        return supportRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(request -> {
                    User user = usersById.get(request.getUserId());
                    String fullName = user == null
                            ? "Неизвестный пользователь"
                            : (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
                    return new PanelSupportRequestDto(
                            request.getId(),
                            request.getUserId(),
                            user != null ? user.getUsername() : null,
                            fullName.isBlank() ? "Неизвестный пользователь" : fullName,
                            emailsByUserId.get(request.getUserId()),
                            request.getContactChannel(),
                            request.getContactValue(),
                            request.getSubject(),
                            request.getMessage(),
                            request.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void deleteById(Long id) {
        if (!supportRequestRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Сообщение не найдено.");
        }
        supportRequestRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        supportRequestRepository.deleteAllInBatch();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validateIncomingRequest(String contactChannel, String contactValue, String subject, String message) {
        if (!ALLOWED_CHANNELS.contains(contactChannel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Выберите корректный канал для ответа.");
        }
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите тему обращения.");
        }
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Введите сообщение.");
        }
        if ("phone".equals(contactChannel) || "whatsapp".equals(contactChannel)) {
            String digits = contactValue == null ? "" : contactValue.replaceAll("\\D", "");
            if (contactValue == null || !PHONE_PATTERN.matcher(contactValue).matches() || digits.length() < 7 || digits.length() > 15) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Для телефона и WhatsApp используйте только цифры и телефонные символы.");
            }
            return;
        }
        if ("telegram".equals(contactChannel) && (contactValue == null || !TELEGRAM_PATTERN.matcher(contactValue).matches())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Укажите корректный Telegram: @username или ID без пробелов.");
        }
        if ("email".equals(contactChannel) && (contactValue == null || !EMAIL_PATTERN.matcher(contactValue).matches())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите корректный email.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
