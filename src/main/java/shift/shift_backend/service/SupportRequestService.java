package shift.shift_backend.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
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

    private final SupportRequestRepository supportRequestRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    @Transactional
    public SupportRequestDto create(Authentication authentication, CreateSupportRequest request) {
        User user = currentUserService.getCurrentUser(authentication);

        SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUserId(user.getId());
        supportRequest.setContactChannel(trim(request.contactChannel()));
        supportRequest.setContactValue(trim(request.contactValue()));
        supportRequest.setSubject(trim(request.subject()));
        supportRequest.setMessage(trim(request.message()));

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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
