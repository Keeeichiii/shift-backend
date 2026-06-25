function getBannerConfig(profile) {
    switch (profile.docStatus) {
        case "VERIFIED":
            return {
                className: "panel-banner panel-banner_success",
                title: "Профиль одобрен",
                text: "Документы подтверждены. Вы можете пользоваться арендой без ограничений."
            };
        case "REJECTED":
            return {
                className: "panel-banner panel-banner_danger",
                title: "Профиль отклонён",
                text: "Проверьте данные водительских прав и обновите профиль перед повторной проверкой."
            };
        case "EXPIRED":
            return {
                className: "panel-banner panel-banner_warning",
                title: "Документы требуют обновления",
                text: "Срок действия документов истёк или требует повторной проверки."
            };
        default:
            return {
                className: "panel-banner panel-banner_pending",
                title: "Профиль ожидает проверки",
                text: profile.licenseSubmittedAt
                    ? "Документы отправлены на модерацию. После проверки администратор обновит данные, а модератор подтвердит доступ."
                    : "Загрузите изображения водительских прав и главной страницы паспорта, чтобы отправить документы на модерацию."
            };
    }
}

function renderBanner(profile) {
    const banner = document.getElementById("accountBanner");
    const title = document.getElementById("accountBannerTitle");
    const text = document.getElementById("accountBannerText");
    const config = getBannerConfig(profile);
    banner.className = config.className;
    title.textContent = config.title;
    text.textContent = config.text;
}

function parseIsoDateOnly(value) {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        return null;
    }
    const [y, m, d] = value.split("-").map(Number);
    return Date.UTC(y, m - 1, d);
}

function isDateBeforeTodayUtc(isoDate) {
    const t = parseIsoDateOnly(isoDate);
    if (t === null) {
        return true;
    }
    const now = new Date();
    const todayUtc = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
    return t < todayUtc;
}

function isBanActive(isoDate) {
    if (!isoDate) {
        return false;
    }
    return !isDateBeforeTodayUtc(isoDate);
}

function getRentAccess(profile) {
    if (isBanActive(profile.drivingBanUntil)) {
        return {
            variant: "denied",
            status: "Нет доступа",
            hint: `Действует лишение права на вождение до ${formatDate(profile.drivingBanUntil)}. После окончания срока доступ может быть восстановлен при действующих правах и одобренном профиле.`
        };
    }

    if (profile.docStatus === "REJECTED") {
        return {
            variant: "denied",
            status: "Отклонён",
            hint: "Проверка документов не пройдена. Загрузите корректные снимки прав с двух сторон и дождитесь повторной модерации."
                + " При необходимости добавьте актуальную главную страницу паспорта."
        };
    }

    if (profile.docStatus === "EXPIRED") {
        return {
            variant: "denied",
            status: "Нет доступа",
            hint: "Документы помечены как устаревшие. Отправьте актуальные фото водительского удостоверения на проверку."
                + " При необходимости обновите и снимок главной страницы паспорта."
        };
    }

    const hasLicenseId = profile.driverLicense && String(profile.driverLicense).trim();
    const licenseExpired = profile.licenseExpiresAt && isDateBeforeTodayUtc(profile.licenseExpiresAt);

    if (profile.docStatus === "VERIFIED") {
        if (licenseExpired) {
            return {
                variant: "denied",
                status: "Нет доступа",
                hint: "Срок действия водительских прав истёк. Загрузите новые снимки удостоверения — после проверки администратор обновит данные."
            };
        }
        return {
            variant: "allowed",
            status: "Разрешён",
            hint: hasLicenseId && profile.licenseExpiresAt
                    ? "Профиль и водительские права в порядке. Можно бронировать автомобиль в рамках правил сервиса."
                    : "Документы подтверждены. Можно бронировать автомобиль в рамках правил сервиса."
        };
    }

    if (licenseExpired) {
        return {
            variant: "denied",
            status: "Нет доступа",
            hint: "Срок действия водительских прав истёк. Загрузите новые снимки удостоверения — после проверки администратор обновит данные."
        };
    }

    if (profile.docStatus === "PENDING") {
        const hasUpload = (profile.licenseFrontImage && String(profile.licenseFrontImage).trim())
                && (profile.licenseBackImage && String(profile.licenseBackImage).trim());
        if (hasUpload || profile.licenseSubmittedAt) {
            return {
                variant: "pending",
                status: "Ожидает проверки",
                hint: "Снимки прав и паспорта отправлены. Модератор проверит изображения, администратор внесёт номер и срок действия прав."
            };
        }
        return {
            variant: "pending",
            status: "Ожидает проверки",
            hint: "Загрузите два снимка водительского удостоверения и главную страницу паспорта, чтобы отправить документы на модерацию."
        };
    }

    return {
        variant: "pending",
        status: "Ожидает проверки",
        hint: "Дождитесь проверки документов или завершите загрузку снимков прав в блоке ниже."
    };
}

function renderRentAccess(profile) {
    const block = document.getElementById("rentAccessBlock");
    const statusEl = document.getElementById("rentAccessStatus");
    const hintEl = document.getElementById("rentAccessHint");
    if (!block || !statusEl || !hintEl) {
        return;
    }
    const access = getRentAccess(profile);
    block.className = `rent-access-card rent-access_${access.variant}`;
    statusEl.textContent = access.status;
    hintEl.textContent = access.hint;
}

function renderProfileInfo(profile) {
    const info = document.getElementById("accountInfo");
    info.innerHTML = `
        <div class="info-item"><span>Пользователь</span><strong>${escapeHtml(profile.firstName || "")} ${escapeHtml(profile.lastName || "")}</strong></div>
        <div class="info-item"><span>Email</span><strong>${escapeHtml(profile.email)}</strong></div>
        <div class="info-item"><span>Роли</span><strong>${escapeHtml(profile.roles.join(", "))}</strong></div>
        <div class="info-item"><span>Документы</span><strong>${escapeHtml(documentStatusLabel(profile.docStatus))}</strong></div>
        <div class="info-item"><span>Номер прав</span><strong>${escapeHtml(profile.driverLicense || "Заполнит администратор")}</strong></div>
        <div class="info-item"><span>Права действуют до</span><strong>${escapeHtml(formatDate(profile.licenseExpiresAt))}</strong></div>
        <div class="info-item"><span>Лишение прав до</span><strong>${escapeHtml(formatDate(profile.drivingBanUntil))}</strong></div>
        <div class="info-item"><span>Последняя активность</span><strong>${escapeHtml(formatDateTime(profile.lastActivity))}</strong></div>
    `;
}

function renderIdentityInfo(profile) {
    const info = document.getElementById("accountIdentity");
    info.innerHTML = `
        <div class="info-item"><span>Username</span><strong>${escapeHtml(profile.username)}</strong></div>
        <div class="info-item"><span>Имя профиля</span><strong>${escapeHtml(profile.profileName || "Не указано")}</strong></div>
        <div class="info-item"><span>Дата регистрации</span><strong>${escapeHtml(formatDate(profile.registrationDate))}</strong></div>
        <div class="info-item"><span>Телефон</span><strong>${escapeHtml(profile.phone || "Не указан")}</strong></div>
        <div class="info-item"><span>Отправлено на проверку</span><strong>${escapeHtml(formatDateTime(profile.licenseSubmittedAt))}</strong></div>
        <div class="info-item"><span>О себе</span><p>${escapeHtml(profile.bio || "Пользователь пока не добавил описание.")}</p></div>
    `;
}

function renderLicensePreview(profile) {
    const preview = document.getElementById("licensePreview");
    const items = [];

    if (profile.licenseFrontImage) {
        items.push(`
            <figure class="license-shot">
                <button
                    class="license-shot__trigger"
                    type="button"
                    aria-label="Открыть лицевую сторону прав"
                    data-image-lightbox-src="${escapeHtml(profile.licenseFrontImage)}"
                    data-image-lightbox-alt="Лицевая сторона прав"
                    data-image-lightbox-caption="Лицевая сторона прав"
                >
                    <img src="${profile.licenseFrontImage}" alt="Лицевая сторона прав">
                </button>
                <figcaption>Лицевая сторона</figcaption>
            </figure>
        `);
    }
    if (profile.licenseBackImage) {
        items.push(`
            <figure class="license-shot">
                <button
                    class="license-shot__trigger"
                    type="button"
                    aria-label="Открыть обратную сторону прав"
                    data-image-lightbox-src="${escapeHtml(profile.licenseBackImage)}"
                    data-image-lightbox-alt="Обратная сторона прав"
                    data-image-lightbox-caption="Обратная сторона прав"
                >
                    <img src="${profile.licenseBackImage}" alt="Обратная сторона прав">
                </button>
                <figcaption>Обратная сторона</figcaption>
            </figure>
        `);
    }
    if (profile.passportMainImage) {
        items.push(`
            <figure class="license-shot">
                <button
                    class="license-shot__trigger"
                    type="button"
                    aria-label="Открыть главную страницу паспорта"
                    data-image-lightbox-src="${escapeHtml(profile.passportMainImage)}"
                    data-image-lightbox-alt="Главная страница паспорта"
                    data-image-lightbox-caption="Главная страница паспорта"
                >
                    <img src="${profile.passportMainImage}" alt="Главная страница паспорта">
                </button>
                <figcaption>Главная страница паспорта</figcaption>
            </figure>
        `);
    }

    preview.innerHTML = items.length
        ? items.join("")
        : `<div class="info-item"><span>Нет файлов</span><strong>Пользователь ещё не загружал изображения прав и паспорта.</strong></div>`;
}

function fileToDataUrl(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result));
        reader.onerror = () => reject(new Error("Не удалось прочитать файл"));
        reader.readAsDataURL(file);
    });
}

function fillProfileForm(profile) {
    const form = document.getElementById("profileForm");
    form.firstName.value = profile.firstName || "";
    form.lastName.value = profile.lastName || "";
    form.phone.value = profile.phone || "";
    form.profileName.value = profile.profileName || "";
    form.bio.value = profile.bio || "";
}

function renderLongBookingOrders(orders) {
    const list = document.getElementById("longBookingOrderList");
    if (!list) {
        return;
    }
    const statusLabels = {
        PENDING: "Ожидает подтверждения",
        CONFIRMED: "Подтверждён",
        CANCELLED: "Отменён"
    };

    if (!orders.length) {
        list.innerHTML = `<div class="trip-card"><strong>Заказов пока нет</strong><p>На странице автомобиля с тарифом «долгое бронирование» нажмите «Оформить заявку».</p></div>`;
        return;
    }

    list.innerHTML = orders.map((o) => `
        <div class="trip-card" data-long-booking-order-id="${escapeHtml(o.id)}">
            <strong>${escapeHtml(o.vehicleTitle)}</strong>
            <p>Статус: ${escapeHtml(statusLabels[o.status] || o.status)}</p>
            <p>Создан: ${escapeHtml(formatDateTime(o.createdAt))}</p>
            <p>Начало: ${o.requestedStartAt ? escapeHtml(formatDateTime(o.requestedStartAt)) : "—"}</p>
            <p>Окончание: ${o.requestedEndAt ? escapeHtml(formatDateTime(o.requestedEndAt)) : "—"}</p>
            <p>Стоимость: ${escapeHtml(formatMoney(o.estimatedPrice))}</p>
            ${o.customerNote ? `<p>Комментарий: ${escapeHtml(o.customerNote)}</p>` : ""}
            <div class="panel-actions">
                <a class="btn btn-small btn-secondary" href="/vehicle.html?slug=${encodeURIComponent(o.vehicleSlug)}">Карточка авто</a>
                ${["PENDING", "CONFIRMED"].includes(o.status) && isDateTimeAfterNow(o.requestedStartAt)
                    ? `<button type="button" class="btn btn-small long-booking-order-cancel-btn">Отменить до начала</button>`
                    : ""}
            </div>
        </div>
    `).join("");

    list.querySelectorAll(".long-booking-order-cancel-btn").forEach((button) => {
        button.addEventListener("click", async () => {
            const orderId = button.closest("[data-long-booking-order-id]")?.dataset.longBookingOrderId;
            if (!orderId) {
                return;
            }
            if (!window.confirm("Отменить заявку до её начала?")) {
                return;
            }
            try {
                setButtonBusy(button, true, "Отмена...");
                await pageRequest(`/api/me/long-booking-orders/${encodeURIComponent(orderId)}/cancel`, {method: "POST"});
                await loadAccountPage();
            } catch (error) {
                window.alert(extractErrorMessage(error, "Не удалось отменить заявку."));
            } finally {
                setButtonBusy(button, false, "Отменить до начала");
            }
        });
    });
}

function renderTrips(trips) {
    const list = document.getElementById("tripList");
    const count = document.getElementById("tripCount");
    count.textContent = String(trips.length);

    if (!trips.length) {
        list.innerHTML = `<div class="trip-card"><strong>Поездок пока нет</strong><p>После первого бронирования история появится здесь.</p></div>`;
        return;
    }

    list.innerHTML = trips.map((trip) => `
        <div class="trip-card">
            <strong>Поездка #${escapeHtml(trip.id)}</strong>
            <p>Автомобиль: ${escapeHtml(trip.vehicleId)}</p>
            <p>Статус: ${escapeHtml(tripStatusLabel(trip.status))}</p>
            <p>Начало: ${escapeHtml(formatDateTime(trip.startTime))}</p>
            <p>Завершение: ${escapeHtml(formatDateTime(trip.endTime))}</p>
        </div>
    `).join("");
}

async function loadAccountPage() {
    try {
        const sessionUser = await loadSessionUser();
        setupPageHeader(sessionUser);
        const [profile, trips, longBookingOrders] = await Promise.all([
            pageRequest("/api/me"),
            pageRequest("/api/me/trips"),
            pageRequest("/api/me/long-booking-orders")
        ]);

        document.getElementById("docStatusValue").textContent = documentStatusLabel(profile.docStatus);
        document.getElementById("licenseValue").textContent = formatDate(profile.licenseExpiresAt);
        document.getElementById("banValue").textContent = formatDate(profile.drivingBanUntil);
        renderBanner(profile);
        renderRentAccess(profile);
        renderProfileInfo(profile);
        renderIdentityInfo(profile);
        renderLicensePreview(profile);
        fillProfileForm(profile);
        renderTrips(trips);
        renderLongBookingOrders(longBookingOrders);
        clearPageError();
    } catch (error) {
        handleProtectedPageError(error, "Не удалось открыть кабинет пользователя.");
    }
}

const profileForm = document.getElementById("profileForm");
if (profileForm) {
    profileForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const status = document.getElementById("profileStatus");
        const data = Object.fromEntries(new FormData(profileForm).entries());
        const submitButton = profileForm.querySelector('button[type="submit"]');
        ["firstName", "lastName", "phone", "profileName", "bio"].forEach((field) => {
            if (typeof data[field] === "string") {
                data[field] = data[field].trim();
            }
        });

        try {
            setButtonBusy(submitButton, true, "Сохранение...");
            status.textContent = "";
            const updated = await pageRequest("/api/me", {
                method: "PUT",
                body: JSON.stringify(data)
            });
            status.textContent = "Профиль обновлён.";
            renderBanner(updated);
            renderRentAccess(updated);
            renderProfileInfo(updated);
            renderIdentityInfo(updated);
            renderLicensePreview(updated);
            fillProfileForm(updated);
            document.getElementById("docStatusValue").textContent = documentStatusLabel(updated.docStatus);
            document.getElementById("licenseValue").textContent = formatDate(updated.licenseExpiresAt);
            document.getElementById("banValue").textContent = formatDate(updated.drivingBanUntil);
        } catch (error) {
            status.textContent = extractErrorMessage(error, "Не удалось сохранить изменения.");
        } finally {
            setButtonBusy(submitButton, false, "Сохранение...");
        }
    });
}

const licenseUploadForm = document.getElementById("licenseUploadForm");
if (licenseUploadForm) {
    licenseUploadForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const status = document.getElementById("licenseUploadStatus");
        const frontFile = document.getElementById("licenseFrontFile").files[0];
        const backFile = document.getElementById("licenseBackFile").files[0];
        const passportFile = document.getElementById("passportMainFile").files[0];
        const submitButton = licenseUploadForm.querySelector('button[type="submit"]');

        if (!frontFile || !backFile || !passportFile) {
            status.textContent = "Загрузите две стороны прав и главную страницу паспорта.";
            return;
        }

        try {
            setButtonBusy(submitButton, true, "Отправка...");
            status.textContent = "Подготавливаем файлы...";
            const [frontImageData, backImageData, passportMainImageData] = await Promise.all([
                fileToDataUrl(frontFile),
                fileToDataUrl(backFile),
                fileToDataUrl(passportFile)
            ]);
            const updated = await pageRequest("/api/me/license-submission", {
                method: "PUT",
                body: JSON.stringify({frontImageData, backImageData, passportMainImageData})
            });
            status.textContent = "Документы отправлены на модерацию.";
            renderBanner(updated);
            renderRentAccess(updated);
            renderProfileInfo(updated);
            renderIdentityInfo(updated);
            renderLicensePreview(updated);
            document.getElementById("docStatusValue").textContent = documentStatusLabel(updated.docStatus);
            document.getElementById("licenseValue").textContent = formatDate(updated.licenseExpiresAt);
            document.getElementById("banValue").textContent = formatDate(updated.drivingBanUntil);
            licenseUploadForm.reset();
        } catch (error) {
            status.textContent = extractErrorMessage(error, "Не удалось отправить документы.");
        } finally {
            setButtonBusy(submitButton, false, "Отправка...");
        }
    });
}

loadAccountPage();

function setupAccountTabs() {
    const profileBtn = document.getElementById("accountTabProfileBtn");
    const ordersBtn = document.getElementById("accountTabOrdersBtn");
    const profilePanel = document.getElementById("accountPanelProfile");
    const ordersPanel = document.getElementById("accountPanelOrders");
    if (!profileBtn || !ordersBtn || !profilePanel || !ordersPanel) {
        return;
    }
    function show(tab) {
        const isProfile = tab === "profile";
        profilePanel.classList.toggle("hidden", !isProfile);
        ordersPanel.classList.toggle("hidden", isProfile);
        profileBtn.classList.toggle("is-active", isProfile);
        ordersBtn.classList.toggle("is-active", !isProfile);
    }
    profileBtn.addEventListener("click", () => show("profile"));
    ordersBtn.addEventListener("click", () => show("orders"));
}

setupAccountTabs();
