package shift.shift_backend.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.domain.enums.VehicleStatus;
import shift.shift_backend.dto.panel.AdminPanelDto;
import shift.shift_backend.dto.panel.ModeratorPanelDto;
import shift.shift_backend.dto.panel.PanelLongBookingOrderDto;
import shift.shift_backend.dto.panel.PanelUserReviewDto;
import shift.shift_backend.dto.support.PanelSupportRequestDto;
import shift.shift_backend.dto.vehicle.VehicleDto;
import shift.shift_backend.mapper.VehicleMapper;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.UserRoleRepository;
import shift.shift_backend.repository.VehicleRepository;

@Service
@RequiredArgsConstructor
public class PanelService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final SupportRequestService supportRequestService;
    private final LongBookingOrderService longBookingOrderService;
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Transactional(readOnly = true)
    public AdminPanelDto getAdminPanel() {
        List<PanelUserReviewDto> users = buildReviewUsers(user -> true);
        List<PanelSupportRequestDto> supportRequests = supportRequestService.findAllForPanels();
        long totalUsers = users.size();
        long activeUsers = users.stream().filter(user -> user.lastActivity() != null).count();
        long pendingModeration = users.stream().filter(user -> user.docStatus() == DocumentStatus.PENDING).count();
        long approvedUsers = users.stream().filter(user -> user.docStatus() == DocumentStatus.VERIFIED).count();
        List<PanelLongBookingOrderDto> pendingLongBookings = longBookingOrderService.listPendingForStaff();
        List<PanelLongBookingOrderDto> confirmedLongBookings = longBookingOrderService.listConfirmedForStaff();
        List<VehicleDto> bookedFleetVehicles = vehicleRepository.findAllByStatus(VehicleStatus.BOOKED).stream()
                .map(vehicleMapper::toDto)
                .toList();
        return new AdminPanelDto(totalUsers, activeUsers, pendingModeration, approvedUsers, users, supportRequests,
                pendingLongBookings, confirmedLongBookings, bookedFleetVehicles);
    }

    @Transactional(readOnly = true)
    public ModeratorPanelDto getModeratorPanel() {
        List<PanelUserReviewDto> users = buildReviewUsers(this::isRegularUser);
        List<PanelSupportRequestDto> supportRequests = supportRequestService.findAllForPanels();
        long readyForApproval = users.stream().filter(PanelUserReviewDto::eligibleForApproval).count();
        long alreadyApproved = users.stream().filter(user -> user.docStatus() == DocumentStatus.VERIFIED).count();
        List<PanelLongBookingOrderDto> pendingLongBookings = longBookingOrderService.listPendingForStaff();
        List<PanelLongBookingOrderDto> confirmedLongBookings = longBookingOrderService.listConfirmedForStaff();
        List<VehicleDto> bookedFleetVehicles = vehicleRepository.findAllByStatus(VehicleStatus.BOOKED).stream()
                .map(vehicleMapper::toDto)
                .toList();
        return new ModeratorPanelDto(users.size(), readyForApproval, alreadyApproved, users, supportRequests,
                pendingLongBookings, confirmedLongBookings, bookedFleetVehicles);
    }

    @Transactional
    public PanelUserReviewDto approveUser(Long userId) {
        User user = getRegularUser(userId);
        if (!isEligibleForApproval(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, buildModerationNote(user));
        }
        user.setDocStatus(DocumentStatus.VERIFIED);
        return toReviewDto(userRepository.save(user), getCredentialByUserId(userId));
    }

    @Transactional
    public PanelUserReviewDto rejectUser(Long userId) {
        User user = getRegularUser(userId);
        user.setDocStatus(DocumentStatus.REJECTED);
        return toReviewDto(userRepository.save(user), getCredentialByUserId(userId));
    }

    private List<PanelUserReviewDto> buildReviewUsers(Predicate<User> filter) {
        Map<Long, Credential> credentialsByUserId = credentialRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Credential::getUserId, credential -> credential));

        Comparator<PanelUserReviewDto> comparator = Comparator
                .comparing((PanelUserReviewDto user) -> moderationPriority(user.docStatus()))
                .thenComparing(PanelUserReviewDto::registrationDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PanelUserReviewDto::id, Comparator.reverseOrder());

        return userRepository.findAll().stream()
                .filter(filter)
                .map(user -> toReviewDto(user, credentialsByUserId.get(user.getId())))
                .sorted(comparator)
                .toList();
    }

    private PanelUserReviewDto toReviewDto(User user, Credential credential) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return new PanelUserReviewDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName() + " " + user.getLastName(),
                credential != null ? credential.getEmail() : null,
                roles,
                user.getRegistrationDate(),
                user.getDriverLicense(),
                user.getLicenseExpiresAt(),
                user.getDrivingBanUntil(),
                user.getLicenseFrontImage(),
                user.getLicenseBackImage(),
                user.getPassportMainImage(),
                user.getLicenseSubmittedAt(),
                user.getDocStatus(),
                user.getLastActivity(),
                isEligibleForApproval(user),
                buildModerationNote(user)
        );
    }

    private boolean isRegularUser(User user) {
        List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
        return roles.contains("USER") && !roles.contains("ADMIN") && !roles.contains("MODERATOR");
    }

    private User getRegularUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!isRegularUser(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This user cannot be moderated");
        }
        return user;
    }

    private Credential getCredentialByUserId(Long userId) {
        return credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credential not found"));
    }

    private boolean isEligibleForApproval(User user) {
        LocalDate today = LocalDate.now();
        boolean hasImages = user.getLicenseFrontImage() != null && !user.getLicenseFrontImage().isBlank()
                && user.getLicenseBackImage() != null && !user.getLicenseBackImage().isBlank()
                && user.getPassportMainImage() != null && !user.getPassportMainImage().isBlank();
        boolean hasLicense = user.getDriverLicense() != null && !user.getDriverLicense().isBlank();
        boolean licenseValid = user.getLicenseExpiresAt() != null && !user.getLicenseExpiresAt().isBefore(today);
        boolean noActiveBan = user.getDrivingBanUntil() == null || user.getDrivingBanUntil().isBefore(today);
        return hasImages && hasLicense && licenseValid && noActiveBan;
    }

    private String buildModerationNote(User user) {
        LocalDate today = LocalDate.now();
        if (user.getLicenseFrontImage() == null || user.getLicenseFrontImage().isBlank()
                || user.getLicenseBackImage() == null || user.getLicenseBackImage().isBlank()) {
            return "Пользователь ещё не отправил два изображения водительских прав";
        }
        if (user.getPassportMainImage() == null || user.getPassportMainImage().isBlank()) {
            return "Пользователь ещё не отправил изображение главной страницы паспорта";
        }
        if (user.getDriverLicense() == null || user.getDriverLicense().isBlank()) {
            return "Администратор ещё не заполнил номер водительских прав";
        }
        if (user.getLicenseExpiresAt() == null) {
            return "Администратор ещё не указал срок действия прав";
        }
        if (user.getLicenseExpiresAt().isBefore(today)) {
            return "Срок действия водительских прав истёк";
        }
        if (user.getDrivingBanUntil() != null && !user.getDrivingBanUntil().isBefore(today)) {
            return "Есть действующее лишение прав";
        }
        return switch (user.getDocStatus()) {
            case VERIFIED -> "Пользователь уже одобрен";
            case REJECTED -> "Ранее пользователь был отклонён";
            case EXPIRED -> "Документы требуют обновления";
            case PENDING -> "Документы готовы к проверке";
        };
    }

    private int moderationPriority(DocumentStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case REJECTED -> 1;
            case EXPIRED -> 2;
            case VERIFIED -> 3;
        };
    }
}
