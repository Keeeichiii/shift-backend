async function pageRequest(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }
    });

    if (!response.ok) {
        const text = await response.text();
        const error = new Error(text || "Request failed");
        error.status = response.status;
        throw error;
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("Content-Type") || "";
    const text = await response.text();
    if (!text.trim()) {
        return null;
    }
    if (contentType.includes("application/json")) {
        return JSON.parse(text);
    }
    return text;
}

function setButtonBusy(button, busy, busyText) {
    if (!button) return;
    if (busy) {
        button.dataset.originalText = button.textContent;
        button.textContent = busyText;
        button.disabled = true;
    } else {
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
        }
        button.disabled = false;
    }
}

function extractErrorMessage(error, fallback) {
    if (!error || !error.message) {
        return fallback;
    }
    return error.message.replace(/^"|"$/g, "") || fallback;
}

function formatDate(value) {
    if (!value) {
        return "Не указано";
    }
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        const [year, month, day] = value.split("-");
        return `${day}.${month}.${year}`;
    }
    return new Date(value).toLocaleDateString("ru-RU");
}

function formatDateTime(value) {
    if (!value) {
        return "Нет активности";
    }
    return new Date(value).toLocaleString("ru-RU");
}

function isDateTimeAfterNow(value) {
    if (!value) {
        return false;
    }
    const date = new Date(value);
    return !Number.isNaN(date.getTime()) && date.getTime() > Date.now();
}

function formatMoney(value) {
    const number = Number(value);
    if (!Number.isFinite(number)) {
        return "Не рассчитана";
    }
    return `${new Intl.NumberFormat("ru-RU", {
        minimumFractionDigits: Number.isInteger(number) ? 0 : 2,
        maximumFractionDigits: 2
    }).format(number)} BYN`;
}

function documentStatusLabel(status) {
    const map = {
        PENDING: "Ожидает проверки",
        VERIFIED: "Одобрен",
        REJECTED: "Отклонён",
        EXPIRED: "Истёк срок"
    };
    return map[status] || status || "—";
}

function vehicleStatusLabel(status) {
    const map = {
        AVAILABLE: "Доступна",
        BOOKED: "Забронирована",
        IN_USE: "В поездке",
        MAINTENANCE: "На обслуживании"
    };
    return map[status] || status || "—";
}

function tripStatusLabel(status) {
    const map = {
        RESERVED: "Забронирована",
        ACTIVE: "Активна",
        PAUSED: "Пауза",
        COMPLETED: "Завершена",
        CANCELED: "Отменена"
    };
    return map[status] || status || "—";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function ensureImageLightboxMarkup() {
    if (document.getElementById("imageLightbox")) {
        return;
    }
    document.body.insertAdjacentHTML("beforeend", `
        <div id="imageLightbox" class="image-lightbox hidden" aria-hidden="true">
            <div class="image-lightbox__backdrop" data-image-lightbox-close="true"></div>
            <div class="image-lightbox__dialog" role="dialog" aria-modal="true" aria-label="Просмотр документа">
                <button class="image-lightbox__close" type="button" aria-label="Закрыть просмотр" data-image-lightbox-close="true">×</button>
                <img id="imageLightboxImg" class="image-lightbox__img" alt="">
                <p id="imageLightboxCaption" class="image-lightbox__caption"></p>
            </div>
        </div>
    `);
}

function closeImageLightbox() {
    const root = document.getElementById("imageLightbox");
    if (!root) {
        return;
    }
    root.classList.add("hidden");
    root.setAttribute("aria-hidden", "true");
    document.body.classList.remove("image-lightbox-open");
}

function openImageLightbox(src, alt = "", caption = "") {
    ensureImageLightboxMarkup();
    const root = document.getElementById("imageLightbox");
    const image = document.getElementById("imageLightboxImg");
    const captionEl = document.getElementById("imageLightboxCaption");
    if (!root || !image || !captionEl) {
        return;
    }
    image.src = src;
    image.alt = alt;
    captionEl.textContent = caption || alt || "";
    root.classList.remove("hidden");
    root.setAttribute("aria-hidden", "false");
    document.body.classList.add("image-lightbox-open");
}

function initImageLightbox() {
    ensureImageLightboxMarkup();
    if (document.body.dataset.imageLightboxBound === "true") {
        return;
    }
    document.body.dataset.imageLightboxBound = "true";

    document.addEventListener("click", (event) => {
        const trigger = event.target.closest("[data-image-lightbox-src]");
        if (trigger) {
            event.preventDefault();
            openImageLightbox(
                trigger.dataset.imageLightboxSrc,
                trigger.dataset.imageLightboxAlt || "",
                trigger.dataset.imageLightboxCaption || ""
            );
            return;
        }
        if (event.target.closest("[data-image-lightbox-close='true']")) {
            closeImageLightbox();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeImageLightbox();
        }
    });
}

/**
 * Путь к файлу из static (как в БД): ведущий слэш, без Windows-слэшей,
 * сегменты URL-кодируются (кириллица, пробелы). http(s)/data не трогаем.
 */
function normalizeStaticAssetPath(path) {
    const raw = String(path ?? "").trim();
    if (!raw) {
        return raw;
    }
    if (/^https?:\/\//i.test(raw) || raw.startsWith("data:") || raw.startsWith("blob:")) {
        return raw;
    }
    let normalized = raw.replace(/\\/g, "/");
    if (!normalized.startsWith("/")) {
        normalized = `/${normalized.replace(/^\/+/, "")}`;
    }
    return normalized
        .split("/")
        .map((segment) => {
            if (segment === "") {
                return "";
            }
            try {
                return encodeURIComponent(decodeURIComponent(segment));
            } catch {
                return encodeURIComponent(segment);
            }
        })
        .join("/");
}

async function uploadVehicleCardImageFile(file, category) {
    const formData = new FormData();
    formData.append("file", file);
    if (category) {
        formData.append("category", String(category));
    }
    const response = await fetch("/api/vehicle-cards/upload-image", {
        method: "POST",
        body: formData,
        credentials: "same-origin"
    });
    if (!response.ok) {
        const text = await response.text();
        const error = new Error(text || "Не удалось загрузить файл.");
        error.status = response.status;
        throw error;
    }
    return response.json();
}

async function loadSessionUser() {
    return pageRequest("/api/auth/me");
}

function findContactsNavAnchor(nav = document.getElementById("menuRight")) {
    if (!nav) {
        return null;
    }
    for (const a of nav.querySelectorAll("a")) {
        const href = (a.getAttribute("href") || "").trim();
        if (href.includes("contacts")) {
            return a;
        }
    }
    return null;
}

/** Гостевые «Войти» / «Регистрация» — сразу перед ссылкой «Контакты» в #menuRight. */
function syncGuestAuthButtonsPlacement() {
    const nav = document.getElementById("menuRight");
    const contact = findContactsNavAnchor(nav);
    if (!nav || !contact) {
        return;
    }
    const login = document.getElementById("pageLoginBtn");
    const reg = document.getElementById("pageRegisterBtn");
    if (login) {
        nav.insertBefore(login, contact);
    }
    if (reg) {
        const lb = document.getElementById("pageLoginBtn");
        if (lb && lb.parentNode === nav) {
            lb.insertAdjacentElement("afterend", reg);
        } else {
            nav.insertBefore(reg, contact);
        }
    }
}

function ensureGuestLoginButton() {
    const nav = document.getElementById("menuRight");
    const contactAnchor = findContactsNavAnchor(nav);
    const authBlock = document.querySelector(".auth-block");
    if (!nav && !authBlock) {
        return null;
    }
    let loginButton = document.getElementById("pageLoginBtn");
    if (!loginButton) {
        loginButton = document.createElement("button");
        loginButton.id = "pageLoginBtn";
        loginButton.type = "button";
        loginButton.className = "btn btn-small";
        loginButton.textContent = "Войти";
        if (nav && contactAnchor) {
            nav.insertBefore(loginButton, contactAnchor);
        } else if (authBlock) {
            authBlock.insertBefore(loginButton, authBlock.firstChild);
        } else {
            return null;
        }
    }
    syncGuestAuthButtonsPlacement();
    if (loginButton.dataset.bound !== "true") {
        loginButton.addEventListener("click", () => {
            window.location.href = "/index.html";
        });
        loginButton.dataset.bound = "true";
    }
    return loginButton;
}

function ensureGuestRegisterButton() {
    const nav = document.getElementById("menuRight");
    const contactAnchor = findContactsNavAnchor(nav);
    const authBlock = document.querySelector(".auth-block");
    if (!nav && !authBlock) {
        return null;
    }
    let registerButton = document.getElementById("pageRegisterBtn");
    if (!registerButton) {
        registerButton = document.createElement("button");
        registerButton.id = "pageRegisterBtn";
        registerButton.type = "button";
        registerButton.className = "btn btn-small btn-secondary";
        registerButton.textContent = "Регистрация";
        const loginButton = document.getElementById("pageLoginBtn");
        if (nav && contactAnchor) {
            if (loginButton && loginButton.parentNode === nav) {
                loginButton.insertAdjacentElement("afterend", registerButton);
            } else {
                nav.insertBefore(registerButton, contactAnchor);
            }
        } else if (loginButton) {
            loginButton.insertAdjacentElement("afterend", registerButton);
        } else if (authBlock) {
            authBlock.insertBefore(registerButton, authBlock.firstChild);
        } else {
            return null;
        }
    }
    syncGuestAuthButtonsPlacement();
    if (registerButton.dataset.bound !== "true") {
        registerButton.addEventListener("click", () => {
            window.location.href = "/index.html?register=1";
        });
        registerButton.dataset.bound = "true";
    }
    return registerButton;
}

async function performLogout() {
    try {
        await pageRequest("/api/auth/logout", {method: "POST"});
    } finally {
        window.dispatchEvent(new CustomEvent("auth-state-changed", {
            detail: {user: null}
        }));
        window.location.replace("/index.html");
    }
}

function setupPageHeader(user) {
    const roleLabel = document.getElementById("pageRoleLabel");
    const logoutButton = document.getElementById("pageLogoutBtn");
    const panelLink = document.getElementById("panelLink");
    const loginButton = ensureGuestLoginButton();

    if (roleLabel) {
        roleLabel.textContent = Array.isArray(user.roles) ? user.roles.join(", ") : "";
    }
    if (logoutButton) {
        logoutButton.classList.remove("hidden");
        if (logoutButton.dataset.bound !== "true") {
            logoutButton.addEventListener("click", async () => {
                setButtonBusy(logoutButton, true, "Выход...");
                await performLogout();
            });
            logoutButton.dataset.bound = "true";
        }
    }
    if (loginButton) {
        loginButton.classList.add("hidden");
    }
    const registerButton = document.getElementById("pageRegisterBtn");
    if (registerButton) {
        registerButton.classList.add("hidden");
    }
    if (panelLink && Array.isArray(user.roles)) {
        panelLink.classList.add("hidden");
        if (user.roles.includes("ADMIN")) {
            panelLink.textContent = "Панель администратора";
            panelLink.href = "/admin.html";
            panelLink.classList.remove("hidden");
        } else if (user.roles.includes("MODERATOR")) {
            panelLink.textContent = "Панель модератора";
            panelLink.href = "/moderator.html";
            panelLink.classList.remove("hidden");
        }
    }
    window.dispatchEvent(new CustomEvent("auth-state-changed", {
        detail: {user}
    }));
}

function setupGuestPageHeader() {
    const panelLink = document.getElementById("panelLink");
    const roleLabel = document.getElementById("pageRoleLabel");
    const logoutButton = document.getElementById("pageLogoutBtn");
    const loginButton = ensureGuestLoginButton();
    const registerButton = ensureGuestRegisterButton();

    if (panelLink) panelLink.classList.add("hidden");
    if (roleLabel) roleLabel.textContent = "";
    if (logoutButton) logoutButton.classList.add("hidden");
    if (loginButton) loginButton.classList.remove("hidden");
    if (registerButton) registerButton.classList.remove("hidden");

    window.dispatchEvent(new CustomEvent("auth-state-changed", {
        detail: {user: null}
    }));
}

function setPageError(message) {
    const state = document.getElementById("pageState");
    const content = document.getElementById("pageContent");
    if (state) {
        state.textContent = message;
        state.classList.remove("hidden");
    }
    if (content) {
        content.classList.add("hidden");
    }
}

function clearPageError() {
    const state = document.getElementById("pageState");
    const content = document.getElementById("pageContent");
    if (state) {
        state.textContent = "";
        state.classList.add("hidden");
    }
    if (content) {
        content.classList.remove("hidden");
    }
}

function handleProtectedPageError(error, fallbackMessage) {
    if (error && (error.status === 401 || error.status === 403)) {
        window.location.href = "/index.html";
        return;
    }
    setPageError(extractErrorMessage(error, fallbackMessage));
}

function longBookingStaffStatusLabel(status) {
    const map = {
        PENDING: "Ожидает подтверждения",
        CONFIRMED: "Подтверждён",
        CANCELLED: "Отменён"
    };
    return map[status] || status || "—";
}

function renderPanelLongBookingStaffSection(prefix, panel, reloadCallback) {
    const pendingEl = document.getElementById(`${prefix}LongBookingPendingList`);
    const confirmedEl = document.getElementById(`${prefix}LongBookingConfirmedList`);
    const fleetEl = document.getElementById(`${prefix}BookedFleetList`);
    if (!pendingEl || !confirmedEl || !fleetEl) {
        return;
    }

    const pending = panel.longBookingOrdersPending || [];
    const confirmed = panel.longBookingOrdersConfirmed || [];
    const fleet = panel.bookedFleetVehicles || [];
    const activeLongBookings = confirmed.filter((order) => !order.requestedEndAt || isDateTimeAfterNow(order.requestedEndAt));

    if (!pending.length) {
        pendingEl.innerHTML = `<div class="trip-card"><strong>Нет заявок</strong><p>Новые заявки на долгое бронирование появятся здесь.</p></div>`;
    } else {
        pendingEl.innerHTML = pending.map((o) => `
            <div class="trip-card" data-long-booking-order-id="${escapeHtml(o.id)}">
                <div class="long-booking-staff-card__head">
                    <img class="long-booking-staff-card__img" src="${escapeHtml(normalizeStaticAssetPath(o.vehicleImagePath))}" alt="">
                    <div>
                        <strong>${escapeHtml(o.vehicleTitle)}</strong>
                        <p>Пользователь: ${escapeHtml(o.username)}${o.userEmail ? ` · ${escapeHtml(o.userEmail)}` : ""}</p>
                        <p>Создан: ${escapeHtml(formatDateTime(o.createdAt))}</p>
                        <p>Начало: ${o.requestedStartAt ? escapeHtml(formatDateTime(o.requestedStartAt)) : "—"}</p>
                        <p>Окончание: ${o.requestedEndAt ? escapeHtml(formatDateTime(o.requestedEndAt)) : "—"}</p>
                        <p>Стоимость: ${escapeHtml(formatMoney(o.estimatedPrice))}</p>
                        <p>Статус: ${escapeHtml(longBookingStaffStatusLabel(o.status))}</p>
                        ${o.customerNote ? `<p>Комментарий: ${escapeHtml(o.customerNote)}</p>` : ""}
                        <p><a class="btn btn-small btn-secondary" href="/vehicle.html?slug=${encodeURIComponent(o.vehicleSlug)}">Страница авто</a></p>
                        <div class="panel-actions">
                            <button type="button" class="btn btn-small long-booking-confirm-btn">Подтвердить</button>
                            <button type="button" class="btn btn-small btn-secondary long-booking-cancel-btn">Отклонить</button>
                        </div>
                    </div>
                </div>
            </div>
        `).join("");

        pendingEl.querySelectorAll(".long-booking-confirm-btn").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const id = btn.closest("[data-long-booking-order-id]").dataset.longBookingOrderId;
                try {
                    setButtonBusy(btn, true, "…");
                    await pageRequest(`/api/moderator/long-booking-orders/${encodeURIComponent(id)}/confirm`, {method: "POST"});
                    await reloadCallback();
                } catch (error) {
                    window.alert(extractErrorMessage(error, "Не удалось подтвердить."));
                } finally {
                    setButtonBusy(btn, false, "Подтвердить");
                }
            });
        });
        pendingEl.querySelectorAll(".long-booking-cancel-btn").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const id = btn.closest("[data-long-booking-order-id]").dataset.longBookingOrderId;
                try {
                    setButtonBusy(btn, true, "…");
                    await pageRequest(`/api/moderator/long-booking-orders/${encodeURIComponent(id)}/cancel`, {method: "POST"});
                    await reloadCallback();
                } catch (error) {
                    window.alert(extractErrorMessage(error, "Не удалось отклонить."));
                } finally {
                    setButtonBusy(btn, false, "Отклонить");
                }
            });
        });
    }

    if (!confirmed.length) {
        confirmedEl.innerHTML = `<div class="trip-card"><strong>Подтверждённых заявок пока нет</strong></div>`;
    } else {
        confirmedEl.innerHTML = confirmed.map((o) => `
            <div class="trip-card" data-long-booking-order-id="${escapeHtml(o.id)}">
                <strong>${escapeHtml(o.vehicleTitle)}</strong>
                <p>${escapeHtml(o.username)} · ${escapeHtml(formatDateTime(o.createdAt))} · ${o.requestedStartAt ? escapeHtml(formatDateTime(o.requestedStartAt)) : "—"} — ${o.requestedEndAt ? escapeHtml(formatDateTime(o.requestedEndAt)) : "—"}</p>
                <p>Стоимость: ${escapeHtml(formatMoney(o.estimatedPrice))}</p>
                ${o.customerNote ? `<p>${escapeHtml(o.customerNote)}</p>` : ""}
                <div class="panel-actions">
                    <a class="btn btn-small btn-neutral" href="/vehicle.html?slug=${encodeURIComponent(o.vehicleSlug)}">Страница авто</a>
                    ${isDateTimeAfterNow(o.requestedStartAt)
                        ? `<button type="button" class="btn btn-small long-booking-confirmed-cancel-btn">Отменить до начала</button>`
                        : `<span class="status-text">После начала заявки отмена недоступна.</span>`}
                </div>
            </div>
        `).join("");

        confirmedEl.querySelectorAll(".long-booking-confirmed-cancel-btn").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const id = btn.closest("[data-long-booking-order-id]").dataset.longBookingOrderId;
                try {
                    setButtonBusy(btn, true, "Отмена...");
                    await pageRequest(`/api/moderator/long-booking-orders/${encodeURIComponent(id)}/cancel`, {method: "POST"});
                    await reloadCallback();
                } catch (error) {
                    window.alert(extractErrorMessage(error, "Не удалось отменить заявку."));
                } finally {
                    setButtonBusy(btn, false, "Отменить до начала");
                }
            });
        });
    }

    if (!fleet.length && !activeLongBookings.length) {
        fleetEl.innerHTML = `<div class="trip-card"><strong>Нет забронированных машин</strong><p>Забронированные авто флота появятся здесь.</p></div>`;
    } else {
        fleetEl.innerHTML = [
            ...activeLongBookings.map((o) => `
            <div class="trip-card">
                <strong>${escapeHtml(o.vehicleTitle)}</strong>
                <p>Долгое бронирование: ${escapeHtml(o.username)}${o.userEmail ? ` · ${escapeHtml(o.userEmail)}` : ""}</p>
                <p>Период: ${o.requestedStartAt ? escapeHtml(formatDateTime(o.requestedStartAt)) : "—"} — ${o.requestedEndAt ? escapeHtml(formatDateTime(o.requestedEndAt)) : "—"}</p>
                <p>Стоимость: ${escapeHtml(formatMoney(o.estimatedPrice))}</p>
            </div>
        `),
            ...fleet.map((v) => `
            <div class="trip-card">
                <strong>#${escapeHtml(v.id)} · ${escapeHtml(v.licensePlate || "—")}</strong>
                <p>VIN: ${escapeHtml(v.vin || "—")}</p>
                <p>Статус: ${escapeHtml(vehicleStatusLabel(v.status))} · бренд ID: ${escapeHtml(String(v.brandId ?? "—"))}</p>
            </div>
        `)
        ].join("");
    }
}

initImageLightbox();
