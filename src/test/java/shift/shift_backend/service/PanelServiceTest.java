package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.domain.enums.VehicleStatus;
import shift.shift_backend.mapper.VehicleMapper;
import shift.shift_backend.dto.user.AdminLicenseUpdateRequest;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.UserRoleRepository;
import shift.shift_backend.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class PanelServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private SupportRequestService supportRequestService;
    @Mock
    private LongBookingOrderService longBookingOrderService;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleMapper vehicleMapper;

    @InjectMocks
    private PanelService panelService;

    @Test
    void moderatorPanelShowsOnlyRegularUsers() {
        User regular = user(1L, "regular", DocumentStatus.PENDING);
        User moderator = user(2L, "moderator", DocumentStatus.PENDING);

        when(userRepository.findAll()).thenReturn(List.of(regular, moderator));
        when(credentialRepository.findAll()).thenReturn(List.of(credential(1L, "regular@mail.com")));
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("USER"));
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("USER", "MODERATOR"));
        stubPanelDependencies();

        var panel = panelService.getModeratorPanel();

        assertThat(panel.totalCandidates()).isEqualTo(1);
        assertThat(panel.users()).extracting("username").containsExactly("regular");
    }

    @Test
    void adminPanelCountsAllUsersByStatusAndActivity() {
        User pending = user(1L, "pending", DocumentStatus.PENDING);
        pending.setLastActivity(OffsetDateTime.now());
        User verified = user(2L, "verified", DocumentStatus.VERIFIED);

        when(userRepository.findAll()).thenReturn(List.of(pending, verified));
        when(credentialRepository.findAll()).thenReturn(List.of(
                credential(1L, "pending@mail.com"),
                credential(2L, "verified@mail.com")
        ));
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("USER"));
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("ADMIN"));
        stubPanelDependencies();

        var panel = panelService.getAdminPanel();

        assertThat(panel.totalUsers()).isEqualTo(2);
        assertThat(panel.activeUsers()).isEqualTo(1);
        assertThat(panel.pendingModeration()).isEqualTo(1);
        assertThat(panel.approvedUsers()).isEqualTo(1);
        assertThat(panel.users()).hasSize(2);
    }

    @Test
    void approveUserRejectsNonRegularUser() {
        User admin = user(2L, "admin", DocumentStatus.PENDING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRoleRepository.findRoleNamesByUserId(2L)).thenReturn(List.of("ADMIN"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> panelService.approveUser(2L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void approveUserRejectsIncompleteDocuments() {
        User regular = user(1L, "regular", DocumentStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(regular));
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("USER"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> panelService.approveUser(1L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).contains("изображения водительских прав");
    }

    @Test
    void approveUserSetsVerifiedWhenDocumentsAreComplete() {
        User regular = user(1L, "regular", DocumentStatus.PENDING);
        regular.setLicenseFrontImage("front");
        regular.setLicenseBackImage("back");
        regular.setPassportMainImage("passport");
        regular.setDriverLicense("77AA123456");
        regular.setLicenseExpiresAt(LocalDate.now().plusYears(1));
        Credential credential = credential(1L, "regular@mail.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(regular));
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("USER"));
        when(userRepository.save(regular)).thenReturn(regular);
        when(credentialRepository.findByUserId(1L)).thenReturn(Optional.of(credential));

        var result = panelService.approveUser(1L);

        assertThat(result.docStatus()).isEqualTo(DocumentStatus.VERIFIED);
        assertThat(regular.getDocStatus()).isEqualTo(DocumentStatus.VERIFIED);
        verify(userRepository).save(regular);
    }

    @Test
    void updateLicenseDataAllowsModeratorToFillRegularUserLicenseData() {
        User regular = user(1L, "regular", DocumentStatus.PENDING);
        Credential credential = credential(1L, "regular@mail.com");
        LocalDate expiresAt = LocalDate.now().plusYears(1);
        LocalDate banUntil = LocalDate.now().minusDays(1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(regular));
        when(userRoleRepository.findRoleNamesByUserId(1L)).thenReturn(List.of("USER"));
        when(userRepository.save(regular)).thenReturn(regular);
        when(credentialRepository.findByUserId(1L)).thenReturn(Optional.of(credential));

        var result = panelService.updateLicenseData(
                1L,
                new AdminLicenseUpdateRequest("77AA123456", expiresAt, banUntil, DocumentStatus.VERIFIED)
        );

        assertThat(result.docStatus()).isEqualTo(DocumentStatus.VERIFIED);
        assertThat(result.driverLicense()).isEqualTo("77AA123456");
        assertThat(regular.getLicenseExpiresAt()).isEqualTo(expiresAt);
        assertThat(regular.getDrivingBanUntil()).isEqualTo(banUntil);
        verify(userRepository).save(regular);
    }

    private void stubPanelDependencies() {
        when(supportRequestService.findAllForPanels()).thenReturn(List.of());
        when(longBookingOrderService.listPendingForStaff()).thenReturn(List.of());
        when(longBookingOrderService.listConfirmedForStaff()).thenReturn(List.of());
        when(vehicleRepository.findAllByStatus(VehicleStatus.BOOKED)).thenReturn(List.of());
    }

    private static Credential credential(Long userId, String email) {
        Credential credential = new Credential();
        credential.setUserId(userId);
        credential.setEmail(email);
        return credential;
    }

    private static User user(Long id, String username, DocumentStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRegistrationDate(LocalDate.of(2026, 1, 1).plusDays(id));
        user.setDocStatus(status);
        return user;
    }
}
