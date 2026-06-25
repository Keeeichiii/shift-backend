package shift.shift_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final int PRICE_STEP_MINUTES = 30;
    private static final Pattern TARIFF_DURATION_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(ч\\.?|час(?:а|ов)?|сут(?:ки|ок)?|д(?:ень|ня|ней)?)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern TARIFF_PRICE_AFTER_SEPARATOR_PATTERN = Pattern.compile("(?:-|—|–|:)\\s*(\\d+(?:[.,]\\d+)?)");
    private static final Pattern TARIFF_PRICE_BEFORE_BYN_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*BYN", Pattern.CASE_INSENSITIVE);

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
        return longBookingOrderRepository
                .findOverlappingForVehicleCard(card.getId(), blockingStatuses(), from, to)
                .stream()
                .map(o -> new LongBookingBusyIntervalDto(o.getRequestedStartAt(), o.getRequestedEndAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LongBookingOrderDto> listForCurrentUser(Authentication authentication) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        OffsetDateTime now = OffsetDateTime.now();
        return longBookingOrderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(order -> shouldExposeOrder(order, now))
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
        if (!requestedEnd.isAfter(requestedStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Окончание бронирования должно быть позже начала.");
        }
        if (requestedEnd.isBefore(requestedStart.plus(MIN_BOOKING_DURATION))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Окончание бронирования должно быть минимум на один час позже начала.");
        }
        if (Duration.between(requestedStart, requestedEnd).compareTo(MAX_BOOKING_DURATION) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Максимальная длительность одной заявки — 90 суток.");
        }

        Long userId = user.getId();
        VehicleCard card = vehicleCardRepository.findBySlugForUpdate(request.vehicleCardSlug().trim())
                .filter(VehicleCard::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Карточка не найдена или снята с публикации."));
        if (!LONG_BOOKING_CATEGORY.equals(card.getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Заказ долгого бронирования доступен только для автомобилей категории «долгое бронирование».");
        }

        List<LongBookingOrder> overlaps = longBookingOrderRepository.findOverlappingForVehicleCard(
                card.getId(), blockingStatuses(), requestedStart, requestedEnd);
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
    }

    @Transactional(readOnly = true)
    public List<PanelLongBookingOrderDto> listPendingForStaff() {
        return longBookingOrderRepository.findAllByStatusOrderByCreatedAtDesc(LongBookingOrderStatus.PENDING).stream()
                .map(this::toPanelDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PanelLongBookingOrderDto> listConfirmedForStaff() {
        return longBookingOrderRepository
                .findAllByStatusAndRequestedEndAtAfterOrderByCreatedAtDesc(
                        LongBookingOrderStatus.CONFIRMED,
                        OffsetDateTime.now()
                )
                .stream()
                .map(this::toPanelDto)
                .toList();
    }

    @Transactional
    public PanelLongBookingOrderDto confirmByStaff(Long orderId) {
        LongBookingOrder order = getOrderEntity(orderId);
        if (order.getStatus() != LongBookingOrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Подтвердить можно только заявку в статусе «ожидает подтверждения».");
        }
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь заявки не найден."));
        assertMayCreateLongBookingOrder(user);
        VehicleCard card = lockVehicleCard(order.getVehicleCard().getId());
        List<LongBookingOrder> overlaps = longBookingOrderRepository.findOverlappingForVehicleCardExcludingOrder(
                order.getId(),
                card.getId(),
                blockingStatuses(),
                order.getRequestedStartAt(),
                order.getRequestedEndAt()
        );
        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Заявка пересекается с уже подтверждённой или ожидающей заявкой. Подтвердить её нельзя.");
        }
        order.setStatus(LongBookingOrderStatus.CONFIRMED);
        return toPanelDto(longBookingOrderRepository.save(order));
    }

    @Transactional
    public PanelLongBookingOrderDto cancelByStaff(Long orderId) {
        LongBookingOrder order = getOrderEntity(orderId);
        if (!mayCancelBeforeStart(order)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Отменить можно заявку в статусе «ожидает подтверждения» или подтверждённую заявку до её начала.");
        }
        order.setStatus(LongBookingOrderStatus.CANCELLED);
        return toPanelDto(longBookingOrderRepository.save(order));
    }

    @Transactional
    public LongBookingOrderDto cancelForCurrentUser(Authentication authentication, Long orderId) {
        LongBookingOrder order = getOrderEntity(orderId);
        Long currentUserId = currentUserService.getCurrentUserId(authentication);
        if (!currentUserId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нельзя отменить чужую заявку.");
        }
        if (!mayCancelBeforeStart(order)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Отменить заявку можно только до её начала.");
        }
        order.setStatus(LongBookingOrderStatus.CANCELLED);
        return toDto(longBookingOrderRepository.save(order));
    }

    private LongBookingOrder getOrderEntity(Long id) {
        return longBookingOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заказ не найден."));
    }

    private VehicleCard lockVehicleCard(Long id) {
        return vehicleCardRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Карточка машины не найдена."));
    }

    private EnumSet<LongBookingOrderStatus> blockingStatuses() {
        return EnumSet.of(LongBookingOrderStatus.PENDING, LongBookingOrderStatus.CONFIRMED);
    }

    private boolean shouldExposeOrder(LongBookingOrder order, OffsetDateTime now) {
        if (order.getStatus() != LongBookingOrderStatus.CONFIRMED) {
            return true;
        }
        return order.getRequestedEndAt() == null || order.getRequestedEndAt().isAfter(now);
    }

    private boolean mayCancelBeforeStart(LongBookingOrder order) {
        if (order.getStatus() == LongBookingOrderStatus.PENDING) {
            return true;
        }
        return order.getStatus() == LongBookingOrderStatus.CONFIRMED
                && order.getRequestedStartAt() != null
                && order.getRequestedStartAt().isAfter(OffsetDateTime.now());
    }

    private BigDecimal estimateLongBookingPrice(LongBookingOrder order) {
        OffsetDateTime start = order.getRequestedStartAt();
        OffsetDateTime end = order.getRequestedEndAt();
        if (start == null || end == null || !end.isAfter(start)) {
            return null;
        }

        int durationMinutes = (int) Math.ceil(Duration.between(start, end).toMillis() / 60_000.0);
        VehicleCard card = order.getVehicleCard();
        List<TariffPackage> packages = parseLongBookingTariffPackages(card);
        if (packages.isEmpty()) {
            BigDecimal minutePrice = card.getPricePerMinute();
            return minutePrice == null
                    ? null
                    : minutePrice.multiply(BigDecimal.valueOf(durationMinutes)).setScale(2, RoundingMode.HALF_UP);
        }

        int durationSteps = ceilDiv(durationMinutes, PRICE_STEP_MINUTES);
        int maxPackageSteps = packages.stream()
                .mapToInt(pkg -> ceilDiv(pkg.durationMinutes(), PRICE_STEP_MINUTES))
                .max()
                .orElse(1);
        int limit = durationSteps + maxPackageSteps;
        int inf = Integer.MAX_VALUE / 4;
        int[] dp = new int[limit + 1];
        Arrays.fill(dp, inf);
        dp[0] = 0;

        for (int step = 0; step <= limit; step++) {
            if (dp[step] == inf) {
                continue;
            }
            for (TariffPackage pkg : packages) {
                int nextStep = Math.min(limit, step + ceilDiv(pkg.durationMinutes(), PRICE_STEP_MINUTES));
                int nextPrice = dp[step] + pkg.priceCents();
                if (nextPrice < dp[nextStep]) {
                    dp[nextStep] = nextPrice;
                }
            }
        }

        int bestStep = durationSteps;
        for (int step = durationSteps + 1; step <= limit; step++) {
            if (dp[step] < dp[bestStep] || (dp[step] == dp[bestStep] && step < bestStep)) {
                bestStep = step;
            }
        }
        return dp[bestStep] == inf ? null : BigDecimal.valueOf(dp[bestStep], 2);
    }

    private List<TariffPackage> parseLongBookingTariffPackages(VehicleCard card) {
        List<TariffPackage> packages = new ArrayList<>();
        parseTariffLines(card.getHourPackagesText(), packages);
        parseTariffLines(card.getDayPackagesText(), packages);
        return packages;
    }

    private void parseTariffLines(String text, List<TariffPackage> packages) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String rawLine : text.split("\\R")) {
            TariffPackage tariffPackage = parseTariffPackageLine(rawLine);
            if (tariffPackage != null) {
                packages.add(tariffPackage);
            }
        }
    }

    private TariffPackage parseTariffPackageLine(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        Matcher durationMatcher = TARIFF_DURATION_PATTERN.matcher(line);
        if (!durationMatcher.find()) {
            return null;
        }
        BigDecimal amount = parseDecimal(durationMatcher.group(1));
        BigDecimal price = parseTariffPrice(line);
        if (amount == null || price == null || amount.signum() <= 0 || price.signum() <= 0) {
            return null;
        }

        String unit = durationMatcher.group(2).toLowerCase();
        BigDecimal minutesMultiplier = unit.startsWith("ч") ? BigDecimal.valueOf(60) : BigDecimal.valueOf(24 * 60);
        int durationMinutes = amount.multiply(minutesMultiplier).setScale(0, RoundingMode.HALF_UP).intValue();
        int priceCents = price.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
        return new TariffPackage(durationMinutes, priceCents);
    }

    private BigDecimal parseTariffPrice(String line) {
        Matcher afterSeparator = TARIFF_PRICE_AFTER_SEPARATOR_PATTERN.matcher(line);
        if (afterSeparator.find()) {
            return parseDecimal(afterSeparator.group(1));
        }
        Matcher beforeByn = TARIFF_PRICE_BEFORE_BYN_PATTERN.matcher(line);
        return beforeByn.find() ? parseDecimal(beforeByn.group(1)) : null;
    }

    private BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
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
                estimateLongBookingPrice(order),
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
                estimateLongBookingPrice(order),
                c.getTitle(),
                c.getSlug(),
                c.getImagePath(),
                c.getCategory()
        );
    }

    private record TariffPackage(int durationMinutes, int priceCents) {
    }
}
