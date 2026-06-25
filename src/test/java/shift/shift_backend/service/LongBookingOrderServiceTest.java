package shift.shift_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.LongBookingOrder;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.domain.entity.VehicleCard;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.domain.enums.LongBookingOrderStatus;
import shift.shift_backend.dto.longbooking.CreateLongBookingOrderRequest;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.LongBookingOrderRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.VehicleCardRepository;

@ExtendWith(MockitoExtension.class)
class LongBookingOrderServiceTest {

    @Mock
    private LongBookingOrderRepository longBookingOrderRepository;
    @Mock
    private VehicleCardRepository vehicleCardRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private LongBookingOrderService longBookingOrderService;

    @Test
    void createAllowsExactlyOneHourBooking() {
        User user = verifiedUser();
        VehicleCard card = longBookingCard();
        OffsetDateTime start = OffsetDateTime.now().plusHours(2);
        OffsetDateTime end = start.plusHours(1);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
        when(vehicleCardRepository.findBySlugForUpdate("toyota-camry")).thenReturn(Optional.of(card));
        when(longBookingOrderRepository.findOverlappingForVehicleCard(eq(20L), anyCollection(), eq(start), eq(end)))
                .thenReturn(List.of());
        when(longBookingOrderRepository.save(any(LongBookingOrder.class))).thenAnswer(invocation -> {
            LongBookingOrder order = invocation.getArgument(0);
            order.setId(30L);
            order.setCreatedAt(OffsetDateTime.now());
            return order;
        });

        var result = longBookingOrderService.create(
                authentication,
                new CreateLongBookingOrderRequest("  toyota-camry  ", start, end, " note ")
        );

        ArgumentCaptor<LongBookingOrder> captor = ArgumentCaptor.forClass(LongBookingOrder.class);
        verify(longBookingOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestedEndAt()).isEqualTo(end);
        assertThat(result.id()).isEqualTo(30L);
        assertThat(result.estimatedPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void createRejectsBookingShorterThanOneHour() {
        User user = verifiedUser();
        OffsetDateTime start = OffsetDateTime.now().plusHours(2);
        OffsetDateTime end = start.plusMinutes(59);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> longBookingOrderService.create(
                        authentication,
                        new CreateLongBookingOrderRequest("toyota-camry", start, end, null)
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(longBookingOrderRepository, never()).save(any());
    }

    @Test
    void createRejectsUnverifiedUser() {
        User user = verifiedUser();
        user.setDocStatus(DocumentStatus.PENDING);
        OffsetDateTime start = OffsetDateTime.now().plusHours(2);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> longBookingOrderService.create(
                        authentication,
                        new CreateLongBookingOrderRequest("toyota-camry", start, start.plusHours(2), null)
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(longBookingOrderRepository, never()).save(any());
    }

    @Test
    void createRejectsOverlappingPeriod() {
        User user = verifiedUser();
        VehicleCard card = longBookingCard();
        OffsetDateTime start = OffsetDateTime.now().plusHours(2);
        OffsetDateTime end = start.plusHours(2);
        LongBookingOrder existing = pendingOrder(31L, card, start.plusMinutes(30), end.plusMinutes(30));

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
        when(vehicleCardRepository.findBySlugForUpdate("toyota-camry")).thenReturn(Optional.of(card));
        when(longBookingOrderRepository.findOverlappingForVehicleCard(eq(20L), anyCollection(), eq(start), eq(end)))
                .thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> longBookingOrderService.create(
                        authentication,
                        new CreateLongBookingOrderRequest("toyota-camry", start, end, null)
                )
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(longBookingOrderRepository, never()).save(any());
    }

    @Test
    void confirmRejectsOverlappingOrder() {
        VehicleCard card = longBookingCard();
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusDays(3);
        LongBookingOrder order = pendingOrder(30L, card, start, end);
        LongBookingOrder existing = pendingOrder(31L, card, start.plusHours(1), end.plusHours(1));

        when(longBookingOrderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(userRepository.findById(10L)).thenReturn(Optional.of(verifiedUser()));
        when(vehicleCardRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(card));
        when(longBookingOrderRepository.findOverlappingForVehicleCardExcludingOrder(
                eq(30L), eq(20L), anyCollection(), eq(start), eq(end)
        )).thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> longBookingOrderService.confirmByStaff(30L)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(order.getStatus()).isEqualTo(LongBookingOrderStatus.PENDING);
        verify(longBookingOrderRepository, never()).save(any());
    }

    @Test
    void cancelForCurrentUserAllowsConfirmedOrderBeforeStart() {
        VehicleCard card = longBookingCard();
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = start.plusDays(3);
        LongBookingOrder order = confirmedOrder(30L, card, start, end);

        when(longBookingOrderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(currentUserService.getCurrentUserId(authentication)).thenReturn(10L);
        when(longBookingOrderRepository.save(any(LongBookingOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = longBookingOrderService.cancelForCurrentUser(authentication, 30L);

        assertThat(result.status()).isEqualTo(LongBookingOrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(LongBookingOrderStatus.CANCELLED);
    }

    @Test
    void cancelForCurrentUserRejectsStartedOrder() {
        VehicleCard card = longBookingCard();
        OffsetDateTime start = OffsetDateTime.now().minusHours(1);
        OffsetDateTime end = OffsetDateTime.now().plusHours(2);
        LongBookingOrder order = confirmedOrder(30L, card, start, end);

        when(longBookingOrderRepository.findById(30L)).thenReturn(Optional.of(order));
        when(currentUserService.getCurrentUserId(authentication)).thenReturn(10L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> longBookingOrderService.cancelForCurrentUser(authentication, 30L)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(longBookingOrderRepository, never()).save(any());
    }

    @Test
    void listConfirmedForStaffSkipsFinishedOrdersAtRepositoryLevel() {
        doReturn(List.of()).when(longBookingOrderRepository)
                .findAllByStatusAndRequestedEndAtAfterOrderByCreatedAtDesc(
                        eq(LongBookingOrderStatus.CONFIRMED),
                        any(OffsetDateTime.class)
                );

        assertThat(longBookingOrderService.listConfirmedForStaff()).isEmpty();

        verify(longBookingOrderRepository)
                .findAllByStatusAndRequestedEndAtAfterOrderByCreatedAtDesc(
                        eq(LongBookingOrderStatus.CONFIRMED),
                        any(OffsetDateTime.class)
                );
    }

    private static User verifiedUser() {
        User user = new User();
        user.setId(10L);
        user.setDocStatus(DocumentStatus.VERIFIED);
        user.setDriverLicense("77AA123456");
        user.setLicenseExpiresAt(OffsetDateTime.now().plusYears(1).toLocalDate());
        return user;
    }

    private static VehicleCard longBookingCard() {
        VehicleCard card = new VehicleCard();
        card.setId(20L);
        card.setTitle("Toyota Camry");
        card.setSlug("toyota-camry");
        card.setImagePath("/images/cars/toyota-camry.png");
        card.setCategory(LongBookingOrderService.LONG_BOOKING_CATEGORY);
        card.setHourPackagesText("1 ч. - 10.00 BYN\n6 ч. - 45.00 BYN");
        card.setDayPackagesText("1 сутки - 100.00 BYN\n3 суток - 270.00 BYN");
        card.setPublished(true);
        return card;
    }

    private static LongBookingOrder pendingOrder(Long id, VehicleCard card, OffsetDateTime start, OffsetDateTime end) {
        LongBookingOrder order = new LongBookingOrder();
        order.setId(id);
        order.setUserId(10L);
        order.setVehicleCard(card);
        order.setStatus(LongBookingOrderStatus.PENDING);
        order.setRequestedStartAt(start);
        order.setRequestedEndAt(end);
        return order;
    }

    private static LongBookingOrder confirmedOrder(Long id, VehicleCard card, OffsetDateTime start, OffsetDateTime end) {
        LongBookingOrder order = pendingOrder(id, card, start, end);
        order.setStatus(LongBookingOrderStatus.CONFIRMED);
        return order;
    }
}
