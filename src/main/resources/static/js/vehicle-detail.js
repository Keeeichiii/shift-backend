const VEHICLE_CATEGORY_LABELS = {
    standard: "Стандарт",
    crossover: "Кроссовер",
    premium: "Премиум",
    minivan: "Минивэн 7 мест",
    exclusive: "Эксклюзив",
    electric: "Электро",
    cabriolet: "Кабриолет",
    offroad: "Внедорожник",
    cargo: "Грузовой",
    long_booking: "Долгое бронирование"
};

const LONG_BOOKING_MONTH_LABELS = [
    "Январь",
    "Февраль",
    "Март",
    "Апрель",
    "Май",
    "Июнь",
    "Июль",
    "Август",
    "Сентябрь",
    "Октябрь",
    "Ноябрь",
    "Декабрь"
];

function isIsoDateStrictlyBeforeToday(iso) {
    if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
        return false;
    }
    const [y, m, d] = iso.split("-").map(Number);
    const cand = Date.UTC(y, m - 1, d);
    const now = new Date();
    const todayUtc = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
    return cand < todayUtc;
}

function isDrivingBanActiveIso(iso) {
    if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
        return false;
    }
    return !isIsoDateStrictlyBeforeToday(iso);
}

function canSubmitLongBookingOrder(sessionUser) {
    if (!sessionUser) {
        return false;
    }
    if (isDrivingBanActiveIso(sessionUser.drivingBanUntil)) {
        return false;
    }
    if (sessionUser.docStatus !== "VERIFIED") {
        return false;
    }
    if (sessionUser.licenseExpiresAt && isIsoDateStrictlyBeforeToday(sessionUser.licenseExpiresAt)) {
        return false;
    }
    if (!sessionUser.driverLicense || !String(sessionUser.driverLicense).trim()) {
        return false;
    }
    return true;
}

function longBookingLicenseNoticeHtml(sessionUser) {
    if (!sessionUser) {
        return "";
    }
    if (isDrivingBanActiveIso(sessionUser.drivingBanUntil)) {
        return "Сейчас действует ограничение по вождению. Оформление заявки недоступно до окончания срока. Подробности — в <a href=\"/account.html\">личном кабинете</a>.";
    }
    if (sessionUser.docStatus !== "VERIFIED") {
        return "Заявку можно отправить только после одобрения водительских прав. Загрузите документы и дождитесь подтверждения в <a href=\"/account.html\">личном кабинете</a>.";
    }
    if (sessionUser.licenseExpiresAt && isIsoDateStrictlyBeforeToday(sessionUser.licenseExpiresAt)) {
        return "Срок действия прав в профиле истёк. Обновите документы в <a href=\"/account.html\">личном кабинете</a>.";
    }
    if (!sessionUser.driverLicense || !String(sessionUser.driverLicense).trim()) {
        return "В профиле не указан номер водительского удостоверения. Дождитесь проверки документов в <a href=\"/account.html\">личном кабинете</a>.";
    }
    return "";
}

const LB_STEP_MS = 30 * 60 * 1000;
const LB_MIN_DURATION_MS = 60 * 60 * 1000;

const longBookingCalState = {
    slug: "",
    viewYear: new Date().getFullYear(),
    viewMonth: new Date().getMonth() + 1,
    busy: [],
    startMs: null,
    endMs: null,
    pickPhase: "start",
    selectedDayMs: null
};

function rangesOverlapMs(a0, a1, b0, b1) {
    return a0 < b1 && b0 < a1;
}

function mergeBusyIntervalsMs(raw) {
    if (!raw.length) {
        return [];
    }
    const sorted = raw
        .map((x) => ({start: Math.min(x.start, x.end), end: Math.max(x.start, x.end)}))
        .filter((x) => Number.isFinite(x.start) && Number.isFinite(x.end) && x.end > x.start)
        .sort((a, b) => a.start - b.start);
    const out = [];
    let cur = {...sorted[0]};
    for (let i = 1; i < sorted.length; i += 1) {
        const n = sorted[i];
        if (n.start <= cur.end) {
            cur.end = Math.max(cur.end, n.end);
        } else {
            out.push(cur);
            cur = {...n};
        }
    }
    out.push(cur);
    return out;
}

function dayBusyKind(dayStartMs, dayEndMs, merged) {
    const total = dayEndMs - dayStartMs;
    let covered = 0;
    for (const b of merged) {
        const a = Math.max(dayStartMs, b.start);
        const z = Math.min(dayEndMs, b.end);
        if (z > a) {
            covered += z - a;
        }
    }
    if (covered <= 0) {
        return "free";
    }
    if (covered >= total * 0.97) {
        return "full";
    }
    return "partial";
}

function localDayBoundsMs(y, monthIndex0, dayOfMonth) {
    const start = new Date(y, monthIndex0, dayOfMonth, 0, 0, 0, 0).getTime();
    const end = new Date(y, monthIndex0, dayOfMonth + 1, 0, 0, 0, 0).getTime();
    return {start, end};
}

function todayLocalMidnightMs() {
    const n = new Date();
    return new Date(n.getFullYear(), n.getMonth(), n.getDate(), 0, 0, 0, 0).getTime();
}

function formatTimeLocal(ms) {
    return new Date(ms).toLocaleTimeString("ru-RU", {hour: "2-digit", minute: "2-digit"});
}

function formatDayTitleLocal(ms) {
    return new Date(ms).toLocaleDateString("ru-RU", {weekday: "long", day: "numeric", month: "long", year: "numeric"});
}

function busyLinesForDay(dayStartMs, dayEndMs, merged) {
    const lines = [];
    for (const b of merged) {
        const a = Math.max(dayStartMs, b.start);
        const z = Math.min(dayEndMs, b.end);
        if (z > a) {
            lines.push(`${formatTimeLocal(a)} — ${formatTimeLocal(z)}`);
        }
    }
    return lines;
}

function rangeOverlapsMerged(startMs, endMs, merged) {
    return merged.some((b) => rangesOverlapMs(startMs, endMs, b.start, b.end));
}

function collectStartSlotOptions(dayStartMs, dayEndMs, merged, nowMs) {
    const slots = [];
    const minT = Math.max(dayStartMs, nowMs + 60_000);
    for (let t = dayStartMs; t < dayEndMs; t += LB_STEP_MS) {
        if (t < minT) {
            continue;
        }
        const needEnd = t + LB_MIN_DURATION_MS;
        if (!rangeOverlapsMerged(t, needEnd, merged)) {
            slots.push(t);
        }
    }
    return slots;
}

function collectEndSlotOptions(dayStartMs, dayEndMs, startMs, merged) {
    const minEnd = startMs + LB_MIN_DURATION_MS;
    const slots = [];
    let t = dayStartMs;
    while (t < dayEndMs && t <= minEnd) {
        t += LB_STEP_MS;
    }
    for (; t <= dayEndMs; t += LB_STEP_MS) {
        if (!rangeOverlapsMerged(startMs, t, merged)) {
            slots.push(t);
        }
    }
    return slots;
}

async function loadLongBookingBusyForMonth(slug, year, month1) {
    const monthStart = new Date(year, month1 - 1, 1, 0, 0, 0, 0);
    const nextMonthStart = new Date(year, month1, 1, 0, 0, 0, 0);
    const from = new Date(monthStart);
    from.setDate(from.getDate() - 14);
    const to = new Date(nextMonthStart);
    to.setDate(to.getDate() + 14);
    const qs = new URLSearchParams({from: from.toISOString(), to: to.toISOString()});
    const rows = await pageRequest(
        `/api/vehicle-cards/public/${encodeURIComponent(slug)}/long-booking-busy-intervals?${qs}`
    );
    longBookingCalState.busy = rows
        .map((r) => ({start: Date.parse(r.startAt), end: Date.parse(r.endAt)}))
        .filter((x) => Number.isFinite(x.start) && Number.isFinite(x.end) && x.end > x.start);
}

function updateLongBookingRangeSummary() {
    const el = document.getElementById("longBookingRangeSummary");
    if (!el) {
        return;
    }
    const {startMs, endMs} = longBookingCalState;
    if (startMs == null) {
        el.textContent = "Шаг 1: выберите день начала в календаре.";
        return;
    }
    if (endMs == null) {
        el.textContent = `Начало: ${formatDayTitleLocal(startMs)} ${formatTimeLocal(startMs)}. Шаг 2: выберите день окончания (не раньше чем через час после начала).`;
        return;
    }
    el.textContent = `Период: ${formatDayTitleLocal(startMs)} ${formatTimeLocal(startMs)} — ${formatDayTitleLocal(endMs)} ${formatTimeLocal(endMs)}`;
}

function renderLongBookingCalendarGrid() {
    const grid = document.getElementById("longBookingCalGrid");
    const label = document.getElementById("longBookingCalMonthLabel");
    if (!grid || !label) {
        return;
    }
    const {viewYear, viewMonth, busy, startMs, endMs} = longBookingCalState;
    const merged = mergeBusyIntervalsMs(busy);
    label.textContent = `${LONG_BOOKING_MONTH_LABELS[viewMonth - 1]} ${viewYear}`;

    const firstDow = (new Date(viewYear, viewMonth - 1, 1).getDay() + 6) % 7;
    const daysInMonth = new Date(viewYear, viewMonth, 0).getDate();
    const cells = [];
    for (let i = 0; i < firstDow; i += 1) {
        cells.push(`<div class="lb-cal__cell lb-cal__cell--pad"></div>`);
    }
    const today0 = todayLocalMidnightMs();
    for (let d = 1; d <= daysInMonth; d += 1) {
        const {start: ds, end: de} = localDayBoundsMs(viewYear, viewMonth - 1, d);
        const kind = dayBusyKind(ds, de, merged);
        const isPast = de <= today0;
        let inRange = false;
        if (startMs != null && endMs != null) {
            inRange = ds < endMs && de > startMs;
        } else if (startMs != null) {
            inRange = ds <= startMs && de > startMs;
        }
        const isStart = startMs != null && ds <= startMs && de > startMs;
        const isEndDay = endMs != null && endMs > ds && endMs <= de;
        const classes = ["lb-cal__cell", "lb-cal__day"];
        if (isPast) {
            classes.push("lb-cal__day--past");
        }
        classes.push(`lb-cal__day--${kind}`);
        if (inRange) {
            classes.push("lb-cal__day--in-range");
        }
        if (isStart) {
            classes.push("lb-cal__day--range-start");
        }
        if (isEndDay) {
            classes.push("lb-cal__day--range-end");
        }
        const disabled = isPast ? " disabled" : "";
        cells.push(
            `<button type="button" class="${classes.join(" ")}" data-lb-day="${viewYear}-${viewMonth}-${d}"${disabled}>${d}</button>`
        );
    }
    grid.innerHTML = cells.join("");

    grid.querySelectorAll(".lb-cal__day:not([disabled])").forEach((btn) => {
        btn.addEventListener("click", () => openLongBookingDayPanel(btn.getAttribute("data-lb-day")));
    });
}

async function openLongBookingDayPanel(dayKey) {
    const detail = document.getElementById("longBookingCalDetail");
    const title = document.getElementById("longBookingCalDetailTitle");
    const busyEl = document.getElementById("longBookingCalBusyLines");
    const sel = document.getElementById("longBookingCalTimeSelect");
    const timeLabel = document.getElementById("longBookingCalTimeLabel");
    const applyBtn = document.getElementById("longBookingCalApplyBtn");
    if (!detail || !title || !busyEl || !sel || !timeLabel || !applyBtn) {
        return;
    }
    const [ys, ms, ds] = dayKey.split("-").map((x) => parseInt(x, 10));
    const {start: dayStart, end: dayEnd} = localDayBoundsMs(ys, ms - 1, ds);
    longBookingCalState.selectedDayMs = dayStart;
    const merged = mergeBusyIntervalsMs(longBookingCalState.busy);
    const nowMs = Date.now();
    const lines = busyLinesForDay(dayStart, dayEnd, merged);

    detail.classList.remove("hidden");
    title.textContent = formatDayTitleLocal(dayStart);

    if (lines.length) {
        busyEl.innerHTML = `<p class="lb-cal__busy-title">Уже занято (другие заявки):</p><ul>${lines
            .map((l) => `<li>${escapeHtml(l)}</li>`)
            .join("")}</ul>`;
    } else {
        busyEl.innerHTML = `<p class="lb-cal__busy-title lb-cal__busy-title--ok">В этот день нет подтверждённых пересечений по графику.</p>`;
    }

    const phase = longBookingCalState.pickPhase;
    if (phase === "start") {
        timeLabel.textContent = "Время начала бронирования";
        const slots = collectStartSlotOptions(dayStart, dayEnd, merged, nowMs);
        sel.innerHTML = slots.length
            ? slots.map((t) => `<option value="${t}">${escapeHtml(formatTimeLocal(t))}</option>`).join("")
            : `<option value="">Нет свободного часа подряд без пересечений с занятыми интервалами</option>`;
        applyBtn.textContent = "Зафиксировать начало";
        applyBtn.disabled = false;
    } else {
        const {startMs} = longBookingCalState;
        if (dayEnd <= startMs) {
            sel.innerHTML = `<option value="">Выберите день окончания не раньше дня начала</option>`;
            applyBtn.textContent = "Зафиксировать окончание";
            timeLabel.textContent = "Время окончания";
            applyBtn.disabled = true;
            return;
        }
        applyBtn.disabled = false;
        timeLabel.textContent = "Время окончания";
        const slots = collectEndSlotOptions(dayStart, dayEnd, startMs, merged);
        sel.innerHTML = slots.length
            ? slots.map((t) => `<option value="${t}">${escapeHtml(formatTimeLocal(t))}</option>`).join("")
            : `<option value="">Нет варианта окончания в этот день</option>`;
        applyBtn.textContent = "Зафиксировать окончание";
    }
}

function resetLongBookingRange() {
    longBookingCalState.startMs = null;
    longBookingCalState.endMs = null;
    longBookingCalState.pickPhase = "start";
    longBookingCalState.selectedDayMs = null;
    const detail = document.getElementById("longBookingCalDetail");
    if (detail) {
        detail.classList.add("hidden");
    }
    updateLongBookingRangeSummary();
    renderLongBookingCalendarGrid();
}

async function initLongBookingCalendar(slug) {
    longBookingCalState.slug = slug;
    longBookingCalState.busy = [];
    const now = new Date();
    longBookingCalState.viewYear = now.getFullYear();
    longBookingCalState.viewMonth = now.getMonth() + 1;
    resetLongBookingRange();

    const root = document.getElementById("longBookingCalendarRoot");
    if (root && root.dataset.lbDelegate !== "1") {
        root.dataset.lbDelegate = "1";
        root.addEventListener("click", async (ev) => {
            const id = ev.target.id;
            const activeSlug = longBookingCalState.slug;
            if (!activeSlug) {
                return;
            }
            if (id === "longBookingCalPrev") {
                longBookingCalState.viewMonth -= 1;
                if (longBookingCalState.viewMonth < 1) {
                    longBookingCalState.viewMonth = 12;
                    longBookingCalState.viewYear -= 1;
                }
                await loadLongBookingBusyForMonth(activeSlug, longBookingCalState.viewYear, longBookingCalState.viewMonth);
                renderLongBookingCalendarGrid();
            } else if (id === "longBookingCalNext") {
                longBookingCalState.viewMonth += 1;
                if (longBookingCalState.viewMonth > 12) {
                    longBookingCalState.viewMonth = 1;
                    longBookingCalState.viewYear += 1;
                }
                await loadLongBookingBusyForMonth(activeSlug, longBookingCalState.viewYear, longBookingCalState.viewMonth);
                renderLongBookingCalendarGrid();
            } else if (id === "longBookingCalClearRange") {
                resetLongBookingRange();
            } else if (id === "longBookingCalApplyBtn") {
                const sel = document.getElementById("longBookingCalTimeSelect");
                if (!sel) {
                    return;
                }
                const v = Number(sel.value);
                if (!Number.isFinite(v)) {
                    return;
                }
                if (longBookingCalState.pickPhase === "start") {
                    longBookingCalState.startMs = v;
                    longBookingCalState.endMs = null;
                    longBookingCalState.pickPhase = "end";
                } else {
                    longBookingCalState.endMs = v;
                }
                updateLongBookingRangeSummary();
                renderLongBookingCalendarGrid();
                document.getElementById("longBookingCalDetail")?.classList.add("hidden");
            }
        });
    }

    await loadLongBookingBusyForMonth(slug, longBookingCalState.viewYear, longBookingCalState.viewMonth);
    renderLongBookingCalendarGrid();
    updateLongBookingRangeSummary();
}

function vehicleCategoryLabel(value) {
    return VEHICLE_CATEGORY_LABELS[value] || value || "Без категории";
}

function linesToList(containerId, text, fallback) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }
    const items = String(text || "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
    const finalItems = items.length ? items : [fallback];
    container.innerHTML = finalItems.map((item) => `<li>${escapeHtml(item)}</li>`).join("");
}

function parseLines(text) {
    return String(text || "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
}

function extractSpecValue(lines, regex, fallback) {
    const line = lines.find((item) => regex.test(item));
    if (!line) {
        return fallback;
    }
    const parts = line.split(":");
    if (parts.length > 1) {
        const value = parts.slice(1).join(":").trim();
        return value || fallback;
    }
    return line;
}

function setVehiclePageError(message) {
    const state = document.getElementById("vehiclePageState");
    const content = document.getElementById("vehiclePageContent");
    if (state) {
        state.textContent = message;
        state.classList.remove("hidden");
    }
    if (content) {
        content.classList.add("hidden");
    }
}

function fillVehiclePage(card, sessionUser) {
    document.title = `${card.title} | FredAvto`;
    document.getElementById("vehicleBreadcrumbs").innerHTML =
        `<a href="/services.html">Тарифы</a> / <a href="/services.html">${escapeHtml(vehicleCategoryLabel(card.category))}</a> / <span>${escapeHtml(card.title)}</span>`;
    document.getElementById("vehicleEyebrow").textContent = `Тарифы / ${vehicleCategoryLabel(card.category)}`;
    document.getElementById("vehicleTitle").textContent = card.title;
    document.getElementById("vehicleSubtitle").textContent = card.shortDescription || "Карточка машины FredAvto.";
    document.getElementById("vehicleDetailDescription").textContent = card.detailDescription || "Описание пока не заполнено.";

    const heroImage = document.getElementById("vehicleHeroImage");
    heroImage.src = normalizeStaticAssetPath(card.imagePath);
    heroImage.alt = card.title;

    const wrapped = card.wrapped === true || card.wrapped === "true";
    document.getElementById("vehicleTags").innerHTML = [
        vehicleCategoryLabel(card.category),
        wrapped ? "С оклейкой" : "Без оклейки",
        card.transmission || "Трансмиссия не указана",
        card.fuelType || "Тип топлива не указан"
    ].map((item) => `<span class="car-tag">${escapeHtml(item)}</span>`).join("");

    const isLongBooking = card.category === "long_booking";
    if (isLongBooking) {
        document.getElementById("vehiclePrice").textContent = "Тарифы: часы и сутки";
        document.getElementById("vehiclePriceHint").textContent =
            "Для долгого бронирования минутная оплата не используется — смотрите почасовые и суточные пакеты ниже.";
        linesToList("vehicleMinutePackages", "", "Минутный тариф для этой категории не предусмотрен.");
    } else {
        document.getElementById("vehiclePrice").textContent = `от ${card.pricePerMinute} BYN / 1 мин.`;
        document.getElementById("vehiclePriceHint").textContent =
            card.shortDescription || "Базовая ставка для аренды автомобиля.";
        linesToList("vehicleMinutePackages", card.minutePackagesText, `1 мин. - ${card.pricePerMinute} BYN`);
    }
    linesToList("vehicleHourPackages", card.hourPackagesText, "Почасовые пакеты пока не указаны.");
    linesToList("vehicleDayPackages", card.dayPackagesText, "Суточные пакеты пока не указаны.");
    linesToList("vehicleConditions", card.conditionsText, "Условия аренды будут добавлены позже.");

    const featureLines = parseLines(card.featuresText);
    const chargingValue = extractSpecValue(featureLines, /заряд/i, "Type-C, Lightning, Micro USB");
    const boosterValue = extractSpecValue(featureLines, /бустер/i, "1");
    const phoneHolderValue = extractSpecValue(featureLines, /держатель|телефон/i, "Есть");

    const visibleFeatures = featureLines.filter((item) => !/(заряд|бустер|держатель|телефон)/i.test(item));
    linesToList("vehicleFeatures", visibleFeatures.join("\n"), "Особенности машины будут добавлены позже.");

    const gearboxNode = document.getElementById("vehicleSpecGearbox");
    const engineNode = document.getElementById("vehicleSpecEngine");
    const chargingNode = document.getElementById("vehicleSpecCharging");
    const boosterNode = document.getElementById("vehicleSpecBooster");
    const phoneHolderNode = document.getElementById("vehicleSpecPhoneHolder");

    if (gearboxNode) gearboxNode.textContent = card.transmission || "Не указано";
    if (engineNode) engineNode.textContent = card.engine || "Не указано";
    if (chargingNode) chargingNode.textContent = chargingValue;
    if (boosterNode) boosterNode.textContent = boosterValue;
    if (phoneHolderNode) phoneHolderNode.textContent = phoneHolderValue;

    initVehicleRateTabs(card);
    void setupLongBookingOrderBlock(card, sessionUser);
}

async function setupLongBookingOrderBlock(card, sessionUser) {
    const block = document.getElementById("longBookingOrderBlock");
    const btn = document.getElementById("longBookingOrderSubmit");
    const note = document.getElementById("longBookingOrderNote");
    const status = document.getElementById("longBookingOrderStatus");
    const licenseNotice = document.getElementById("longBookingOrderLicenseNotice");
    if (!block || !btn || !note || !status) {
        return;
    }
    if (!sessionUser || card.category !== "long_booking") {
        block.classList.add("hidden");
        return;
    }
    block.classList.remove("hidden");
    try {
        await initLongBookingCalendar(card.slug);
    } catch (e) {
        status.textContent = extractErrorMessage(e, "Не удалось загрузить занятость календаря.");
    }
    note.value = "";
    status.textContent = "";

    const allowed = canSubmitLongBookingOrder(sessionUser);
    if (licenseNotice) {
        if (allowed) {
            licenseNotice.classList.add("hidden");
            licenseNotice.innerHTML = "";
        } else {
            licenseNotice.classList.remove("hidden");
            licenseNotice.innerHTML = longBookingLicenseNoticeHtml(sessionUser);
        }
    }

    const fresh = btn.cloneNode(true);
    btn.parentNode.replaceChild(fresh, btn);
    fresh.disabled = false;

    fresh.addEventListener("click", async () => {
        status.textContent = "";
        if (!canSubmitLongBookingOrder(sessionUser)) {
            status.textContent = "Сначала пройдите проверку документов в личном кабинете.";
            return;
        }
        const {startMs, endMs} = longBookingCalState;
        if (startMs == null || endMs == null) {
            status.textContent = "Выберите начало и окончание бронирования в календаре.";
            return;
        }
        if (endMs <= startMs + LB_MIN_DURATION_MS) {
            status.textContent = "Окончание должно быть минимум на час позже начала.";
            return;
        }
        if (startMs <= Date.now() - 60_000) {
            status.textContent = "Начало бронирования должно быть в будущем.";
            return;
        }
        const merged = mergeBusyIntervalsMs(longBookingCalState.busy);
        if (rangeOverlapsMerged(startMs, endMs, merged)) {
            status.textContent = "Выбранный период пересекается с занятым интервалом. Обновите календарь и выберите снова.";
            try {
                await loadLongBookingBusyForMonth(card.slug, longBookingCalState.viewYear, longBookingCalState.viewMonth);
                renderLongBookingCalendarGrid();
            } catch {
                /* ignore */
            }
            return;
        }
        try {
            setButtonBusy(fresh, true, "Отправка…");
            await pageRequest("/api/me/long-booking-orders", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    vehicleCardSlug: card.slug,
                    requestedStartAt: new Date(startMs).toISOString(),
                    requestedEndAt: new Date(endMs).toISOString(),
                    customerNote: note.value.trim() || null
                })
            });
            status.textContent = "Заявка создана. Смотрите раздел «Заказы» в кабинете.";
            note.value = "";
            longBookingCalState.busy = [];
            await initLongBookingCalendar(card.slug);
        } catch (error) {
            status.textContent = extractErrorMessage(error, "Не удалось отправить заявку.");
        } finally {
            setButtonBusy(fresh, false, "Оформить заявку");
        }
    });
}

async function initVehicleDetailPage() {
    let sessionUser = null;
    try {
        sessionUser = await loadSessionUser();
        setupPageHeader(sessionUser);
    } catch {
        setupGuestPageHeader();
    }

    const params = new URLSearchParams(window.location.search);
    const slug = params.get("slug");
    if (!slug) {
        setVehiclePageError("Не указан slug карточки машины.");
        return;
    }

    try {
        const card = await pageRequest(`/api/vehicle-cards/public/${encodeURIComponent(slug)}`);
        fillVehiclePage(card, sessionUser);
    } catch (error) {
        setVehiclePageError(extractErrorMessage(error, "Не удалось загрузить карточку машины."));
    }
}

function initVehicleRateTabs(card) {
    const tablist = document.querySelector(".car-rate-tabs");
    if (!tablist) {
        return;
    }
    const isLong = card && card.category === "long_booking";
    const tabs = [...tablist.querySelectorAll(".car-rate-tab")];
    const panels = [...document.querySelectorAll("[data-rate-panel]")];
    if (!tabs.length || !panels.length) {
        return;
    }

    const minTab = tabs.find((t) => t.getAttribute("data-rate") === "minutes");
    const minPanel = panels.find((p) => p.getAttribute("data-rate-panel") === "minutes");
    if (minTab) {
        minTab.classList.toggle("hidden", isLong);
        minTab.hidden = isLong;
    }
    if (minPanel) {
        minPanel.classList.toggle("hidden", isLong);
        minPanel.hidden = isLong;
    }

    function activate(rate) {
        let r = rate;
        if (isLong && r === "minutes") {
            r = "hours";
        }
        tabs.forEach((tab) => {
            if (tab.classList.contains("hidden")) {
                return;
            }
            const on = tab.getAttribute("data-rate") === r;
            tab.classList.toggle("is-active", on);
            tab.setAttribute("aria-selected", on ? "true" : "false");
        });
        panels.forEach((panel) => {
            if (panel.classList.contains("hidden")) {
                return;
            }
            const on = panel.getAttribute("data-rate-panel") === r;
            panel.classList.toggle("is-active", on);
            panel.hidden = !on;
        });
    }

    if (tablist.dataset.rateTabsBound !== "1") {
        tablist.dataset.rateTabsBound = "1";
        tabs.forEach((tab) => {
            tab.addEventListener("click", () => activate(tab.getAttribute("data-rate")));
        });
    }

    activate(isLong ? "hours" : "minutes");
}

initVehicleDetailPage();
