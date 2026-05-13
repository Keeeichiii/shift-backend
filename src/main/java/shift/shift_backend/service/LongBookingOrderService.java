package shift.shift_backend.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shift.shift_backend.domain.entity.Credential;
import shift.shift_backend.domain.entity.LongBookingOrder;
import shift.shift_backend.domain.entity.User;
import shift.shift_backend.domain.entity.VehicleCard;
import shift.shift_backend.domain.enums.DocumentStatus;
import shift.shift_backend.domain.enums.LongBookingOrderStatus;
import shift.shift_backend.dto.longbooking.CreateLongBookingOrderRequest;
import shift.shift_backend.dto.longbooking.LongBookingBusyIntervalDto;
import shift.shift_backend.dto.longbooking.LongBookingOrderDto;
import shift.shift_backend.dto.panel.PanelLongBookingOrderDto;
import shift.shift_backend.repository.CredentialRepository;
import shift.shift_backend.repository.LongBookingOrderRepository;
import shift.shift_backend.repository.UserRepository;
import shift.shift_backend.repository.VehicleCardRepository;

@Service
@RequiredArgsConstructor
public class LongBookingOrderService {

    public static final String LONG_BOOKING_CATEGORY = "long_booking";

    private static final int MAX_BUSY_QUERY_DAYS = 124;
    private static final Duration MIN_BOOKING_DURATION = Duration.ofHours(1);
    private static final Duration MAX_BOOKING_DURATION = Duration.ofDays(90);

    private final LongBookingOrderRepository longBookingOrderRepository;
    private final VehicleCardRepository vehicleCardRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    @Transactional(readOnly = true)
    public List<LongBookingBusyIntervalDto> listBusyIntervalsForPublishedCard(String slug, OffsetDateTime from, OffsetDateTime to) {
        if (!from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Параметр «с» должен быть раньше «по».");
        }
        if (Duration.between(from, to).compareTo(Duration.ofDays(MAX_BUSY_QUERY_DAYS)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Интервал запроса слишком большой.");
        }
        VehicleCard card = vehicleCardRepository.findBySlug(slug.trim())
                .filter(VehicleCard::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Карточка не найдена или снята с публикации."));
        if (!LONG_BOOKING_CATEGORY.equals(card.getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Календарь занятости доступен только для автомобилей категории «долгое бронирование».");
        }
        var blockingStatuses = EnumSet.of(LongBookingOrderStatus.PENDING, LongBookingOrderStatus.CONFIRMED);
        return longBookingOrderRepository
                .findOverlappingForVehicleCard(card.getId(), blockingStatuses, from, to)
                .stream()
                .map(o -> new LongBookingBusyIntervalDto(o.getRequestedStartAt(), o.getRequestedEndAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LongBookingOrderDto> listForCurrentUser(Authentication authentication) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        return longBookingOrderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public LongBookingOrderDto create(Authentication authentication, CreateLongBookingOrderRequest request) {
        User user = currentUserService.getCurrentUser(authentication);
        assertMayCreateLongBookingOrder(user);

        OffsetDateTime requestedStart = request.requestedStartAt();
        OffsetDateTime requestedEnd = request.requestedEndAt();
        if (!requestedStart.isAfter(OffsetDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Укажите дату и время начала бронирования в будущем.");
        }
        if (!requestedEnd.isAfter(requestedStart.plus(MIN_BOOKING_DURATION))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Окончание бронирования должно быть минимум на один час позже начала.");
        }
        if (Duration.between(requestedStart, requestedEnd).compareTo(MAX_BOOKING_DURATION) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Максимальная длительность одной заявки — 90 суток.");
        }

        Long userId = user.getId();
        VehicleCard card = vehicleCardRepository.findBySlug(request.vehicleCardSlug().trim())
                .filter(VehicleCard::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Карточка не найдена или снята с публикации."));
        if (!LONG_BOOKING_CATEGORY.equals(card.getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Заказ долгого бронирования доступен только для автомобилей категории «долгое бронирование».");
        }

        var blockingStatuses = EnumSet.of(LongBookingOrderStatus.PENDING, LongBookingOrderStatus.CONFIRMED);
        List<LongBookingOrder> overlaps = longBookingOrderRepository.findOverlappingForVehicleCard(
                card.getId(), blockingStatuses, requestedStart, requestedEnd);
        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Выбранный период пересекается с уже существующей заявкой (ожидающей подтверждения или подтверждённой). Выберите другие даты.");
        }

        LongBookingOrder order = new LongBookingOrder();
        order.setUserId(userId);
        order.setVehicleCard(card);
        order.setStatus(LongBookingOrderStatus.PENDING);
        order.setRequestedStartAt(requestedStart);
        order.setRequestedEndAt(requestedEnd);
        String note = request.customerNote();
        order.setCustomerNote(note != null && !note.isBlank() ? note.trim() : null);

        return toDto(longBookingOrderRepository.save(order));
    }

    private void assertMayCreateLongBookingOrder(User user) {
        LocalDate today = LocalDate.now();
        if (user.getDrivingBanUntil() != null && !user.getDrivingBanUntil().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Нельзя оставить заявку при действующем лишении права на вождение.");
        }
        if (user.getDocStatus() != DocumentStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Заявку на долгое бронирование можно оставить только при одобренных водительских правах. "
                            + "Загрузите документы в личном кабинете и дождитесь проверки.");
        }
        if (user.getLicenseExpiresAt() != null && user.getLicenseExpiresAt().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Срок действия водительских прав истёк. Обновите данные в личном кабинете.");
        }
        if (user.getDriverLicense() == null || user.getDriverLicense().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "В профиле не указан номер водительского удостоверения. Дождитесь завершения проверки документов.");
        }
    }

    @Transactional(readOnly = true)
    public List<PanelLongBookingOrderDto> listPendingForStaff() {
        return longBookingOrderRepository.findAllByStatusOrderByCreatedAtDesc(LongBookingOrderStatus.PENDING).stream()
                .map(this::toPanelDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PanelLongBookingOrderDto> listConfirmedForStaff() {
        return longBookingOrderRepository.findAllByStatusOrderByCreatedAtDesc(LongBookingOrderStatus.CONFIRMED).stream()
                .map(this::toPanelDto)
                .toList();
    }

    @Transactional
    public PanelLongBookingOrderDto confirmByStaff(Long orderId) {
        LongBookingOrder order = getOrderEntity(orderId);
        if (order.getStatus() != LongBookingOrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Подтвердить можно только заявку в статусе «ожидает подтверждения».");
        }
        order.setStatus(LongBookingOrderStatus.CONFIRMED);
        return toPanelDto(longBookingOrderRepository.save(order));
    }

    @Transactional
    public PanelLongBookingOrderDto cancelByStaff(Long orderId) {
        LongBookingOrder order = getOrderEntity(orderId);
        if (order.getStatus() != LongBookingOrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Отменить можно только заявку в статусе «ожидает подтверждения».");
        }
        order.setStatus(LongBookingOrderStatus.CANCELLED);
        return toPanelDto(longBookingOrderRepository.save(order));
    }

    private LongBookingOrder getOrderEntity(Long id) {
        return longBookingOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден."));
    }

    private PanelLongBookingOrderDto toPanelDto(LongBookingOrder order) {
        VehicleCard c = order.getVehicleCard();
        User user = userRepository.findById(order.getUserId()).orElse(null);
        String username = user != null ? user.getUsername() : "—";
        String email = credentialRepository.findByUserId(order.getUserId()).map(Credential::getEmail).orElse(null);
        return new PanelLongBookingOrderDto(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getRequestedStartAt(),
                order.getRequestedEndAt(),
                order.getCustomerNote(),
                c.getTitle(),
                c.getSlug(),
                c.getImagePath(),
                order.getUserId(),
                username,
                email
        );
    }

    private LongBookingOrderDto toDto(LongBookingOrder order) {
        VehicleCard c = order.getVehicleCard();
        return new LongBookingOrderDto(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getRequestedStartAt(),
                order.getRequestedEndAt(),
                order.getCustomerNote(),
                c.getTitle(),
                c.getSlug(),
                c.getImagePath(),
                c.getCategory()
        );
    }
}
