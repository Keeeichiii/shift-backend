 function setModeratorFlash(message, tone = "neutral") {
    const flash = document.getElementById("moderatorFlash");
    if (!flash) {
        return;
    }
    if (!message) {
        flash.textContent = "";
        flash.className = "panel-inline-status hidden";
        return;
    }
    flash.textContent = message;
    flash.className = `panel-inline-status panel-inline-status_${tone}`;
}

let moderatorVehicleCardManager = null;
let moderatorNewsManager = null;
let moderatorUsers = [];
let selectedModeratorUserId = null;
let moderatorLicenseTab = "queue";

function setModeratorSupportEmptyState() {
    const list = document.getElementById("moderatorSupportRequestList");
    if (!list) {
        return;
    }
    list.innerHTML = `<div class="support-message-card__empty"><strong>Сообщений пока нет.</strong><p>Новые обращения из формы техподдержки появятся здесь.</p></div>`;
}

const MODERATOR_TABS = [
    ["moderator-license-section", "Одобрение данных водительских прав"],
    ["moderator-long-booking-section", "Подтверждение заказов"],
    ["moderator-vehicles-section", "Создание карточки машины"],
    ["moderator-news-section", "Новости"],
    ["moderator-support-section", "Сообщения"]
];

function supportContactChannelLabel(channel) {
    const labels = {
        phone: "Телефон",
        telegram: "Telegram",
        whatsapp: "WhatsApp",
        email: "Email"
    };
    return labels[channel] || channel || "Не указан";
}

function ensureModeratorTabs() {
    const tabsRoot = document.querySelector('[data-panel-tabs="moderator"]');
    if (!tabsRoot) {
        return null;
    }

    MODERATOR_TABS.forEach(([targetId, label], index) => {
        if (tabsRoot.querySelector(`[data-panel-target="${targetId}"]`)) {
            return;
        }
        const tab = document.createElement("button");
        tab.className = `panel-tab${index === 0 ? " is-active" : ""}`;
        tab.type = "button";
        tab.dataset.panelTarget = targetId;
        tab.textContent = label;
        tabsRoot.appendChild(tab);
    });

    return tabsRoot;
}

function renderModeratorSupportRequests(requests) {
    const list = document.getElementById("moderatorSupportRequestList");
    if (!list) {
        return;
    }

    if (!requests.length) {
        setModeratorSupportEmptyState();
        return;
    }

    list.innerHTML = requests.map((request) => `
        <article class="support-message-card" data-support-request-id="${escapeHtml(request.id)}">
            <div class="support-message-card__head">
                <div>
                    <strong>${escapeHtml(request.subject)}</strong>
                    <p>${escapeHtml(request.fullName)}${request.username ? ` (@${escapeHtml(request.username)})` : ""}</p>
                </div>
                <div class="support-message-card__meta">
                    <span class="badge badge_role">${escapeHtml(supportContactChannelLabel(request.contactChannel))}</span>
                    <span class="badge badge_role">${escapeHtml(formatDateTime(request.createdAt))}</span>
                </div>
            </div>
            <div class="support-message-card__body">
                <p>${escapeHtml(request.email || "Email не указан")}</p>
                <p><strong>Контакт для ответа:</strong> ${escapeHtml(request.contactValue || "Не указан")}</p>
                <p>${escapeHtml(request.message)}</p>
            </div>
            <div class="panel-actions">
                <button type="button" class="btn btn-small btn-secondary moderator-support-delete-btn">Удалить сообщение</button>
            </div>
        </article>
    `).join("");

    list.querySelectorAll(".moderator-support-delete-btn").forEach((button) => {
        button.addEventListener("click", async () => {
            const requestId = button.closest("[data-support-request-id]")?.dataset.supportRequestId;
            if (!requestId) {
                return;
            }
            await deleteModeratorSupportRequest(requestId, button);
        });
    });
}

async function deleteModeratorSupportRequest(requestId, button) {
    if (!window.confirm("Удалить это сообщение?")) {
        return;
    }
    const status = document.getElementById("moderatorSupportRequestStatus");
    try {
        setButtonBusy(button, true, "Удаление...");
        if (status) {
            status.textContent = "";
        }
        await pageRequest(`/api/support-requests/${encodeURIComponent(requestId)}`, {method: "DELETE"});
        button.closest("[data-support-request-id]")?.remove();
        if (!document.querySelector("#moderatorSupportRequestList [data-support-request-id]")) {
            setModeratorSupportEmptyState();
        }
        if (status) {
            status.textContent = "Сообщение удалено.";
        }
    } catch (error) {
        if (status) {
            status.textContent = extractErrorMessage(error, "Не удалось удалить сообщение.");
        }
    } finally {
        setButtonBusy(button, false, "Удалить сообщение");
    }
}

async function clearModeratorSupportRequests(button = document.getElementById("moderatorSupportRequestClear")) {
    if (!window.confirm("Очистить все сообщения?")) {
        return;
    }
    const status = document.getElementById("moderatorSupportRequestStatus");
    try {
        setButtonBusy(button, true, "Очистка...");
        if (status) {
            status.textContent = "";
        }
        await pageRequest("/api/support-requests", {method: "DELETE"});
        setModeratorSupportEmptyState();
        if (status) {
            status.textContent = "Сообщения очищены.";
        }
    } catch (error) {
        if (status) {
            status.textContent = extractErrorMessage(error, "Не удалось очистить сообщения.");
        }
    } finally {
        setButtonBusy(button, false, "Очистить сообщения");
    }
}

function renderLicenseImages(containerId, user) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }
    const items = [];

    if (user?.licenseFrontImage) {
        items.push(`
            <figure class="license-shot">
                <button
                    class="license-shot__trigger"
                    type="button"
                    aria-label="Открыть лицевую сторону прав"
                    data-image-lightbox-src="${escapeHtml(user.licenseFrontImage)}"
                    data-image-lightbox-alt="Лицевая сторона прав"
                    data-image-lightbox-caption="Лицевая сторона прав"
                >
                    <img src="${user.licenseFrontImage}" alt="Лицевая сторона прав">
                </button>
                <figcaption>Лицевая сторона</figcaption>
            </figure>
        `);
    }
    if (user?.licenseBackImage) {
        items.push(`
            <figure class="license-shot">
                <button
                    class="license-shot__trigger"
                    type="button"
                    aria-label="Открыть обратную сторону прав"
                    data-image-lightbox-src="${escapeHtml(user.licenseBackImage)}"
                    data-image-lightbox-alt="Обратная сторона прав"
                    data-image-lightbox-caption="Обратная сторона прав"
                >
                    <img src="${user.licenseBackImage}" alt="Обратная сторона прав">
                </button>
                <figcaption>Обратная сторона</figcaption>
            </figure>
        `);
    }
    if (user?.passportMainImage) {
        items.push(`
            <figure class="license-shot">
                <button
                    class="license-shot__trigger"
                    type="button"
                    aria-label="Открыть главную страницу паспорта"
                    data-image-lightbox-src="${escapeHtml(user.passportMainImage)}"
                    data-image-lightbox-alt="Главная страница паспорта"
                    data-image-lightbox-caption="Главная страница паспорта"
                >
                    <img src="${user.passportMainImage}" alt="Главная страница паспорта">
                </button>
                <figcaption>Главная страница паспорта</figcaption>
            </figure>
        `);
    }

    container.innerHTML = items.length
        ? items.join("")
        : `<div class="info-item"><span>Нет изображений</span><strong>Пользователь ещё не загрузил права и паспорт.</strong></div>`;
}

function fillModeratorLicenseForm(user) {
    selectedModeratorUserId = user.id;
    document.getElementById("moderatorSelectedUser").value = `${user.fullName} (@${user.username})`;
    document.getElementById("moderatorDriverLicense").value = user.driverLicense || "";
    document.getElementById("moderatorLicenseExpiresAt").value = user.licenseExpiresAt || "";
    document.getElementById("moderatorDrivingBanUntil").value = user.drivingBanUntil || "";
    document.getElementById("moderatorDocStatus").value = user.docStatus || "PENDING";
    renderLicenseImages("moderatorLicensePreview", user);
    const status = document.getElementById("moderatorSaveStatus");
    if (status) {
        status.textContent = "";
    }
}

function clearModeratorLicenseForm() {
    selectedModeratorUserId = null;
    document.getElementById("moderatorSelectedUser").value = "";
    document.getElementById("moderatorDriverLicense").value = "";
    document.getElementById("moderatorLicenseExpiresAt").value = "";
    document.getElementById("moderatorDrivingBanUntil").value = "";
    document.getElementById("moderatorDocStatus").value = "PENDING";
    renderLicenseImages("moderatorLicensePreview", null);
}

function renderModeratorUsers(users) {
    const list = document.getElementById("reviewList");
    if (!list) {
        return;
    }

    const query = String(document.getElementById("moderatorLicenseSearch")?.value || "").trim().toLowerCase();
    const sort = document.getElementById("moderatorLicenseSort")?.value || "priority";
    const visibleUsers = users
        .filter((user) => moderatorLicenseTab === "approved" ? user.docStatus === "VERIFIED" : user.docStatus !== "VERIFIED")
        .filter((user) => {
            if (!query) {
                return true;
            }
            return [
                user.fullName,
                user.username,
                user.email,
                documentStatusLabel(user.docStatus)
            ].some((value) => String(value || "").toLowerCase().includes(query));
        })
        .sort((left, right) => compareModeratorLicenseUsers(left, right, sort));

    document.querySelectorAll("[data-moderator-license-tab]").forEach((button) => {
        const tab = button.dataset.moderatorLicenseTab;
        button.classList.toggle("is-active", tab === moderatorLicenseTab);
        const count = tab === "approved"
            ? users.filter((user) => user.docStatus === "VERIFIED").length
            : users.filter((user) => user.docStatus !== "VERIFIED").length;
        button.textContent = `${tab === "approved" ? "Одобренные" : "На проверку"} (${count})`;
    });

    if (!visibleUsers.length) {
        const emptyText = moderatorLicenseTab === "approved"
            ? "Одобренных пользователей пока нет."
            : "Нет пользователей в очереди проверки.";
        list.innerHTML = `<div class="review-card"><strong>Список пуст</strong><p>${emptyText}</p></div>`;
        return;
    }

    list.innerHTML = visibleUsers.map((user) => `
        <div class="review-card" data-user-id="${escapeHtml(user.id)}">
            <strong>${escapeHtml(user.fullName)} <small>@${escapeHtml(user.username)}</small></strong>
            <p>${escapeHtml(user.email || "Email не указан")}</p>
            <div class="meta-badges">
                <span class="badge badge_status_${String(user.docStatus).toLowerCase()}">${escapeHtml(documentStatusLabel(user.docStatus))}</span>
                <span class="badge badge_role">Права до ${escapeHtml(formatDate(user.licenseExpiresAt))}</span>
                <span class="badge badge_role">Лишение до ${escapeHtml(formatDate(user.drivingBanUntil))}</span>
            </div>
            <p>${escapeHtml(user.moderationNote)}</p>
            <div class="panel-actions">
                <button class="btn btn-small select-moderator-license-user" type="button">Выбрать</button>
            </div>
        </div>
    `).join("");

    list.querySelectorAll(".select-moderator-license-user").forEach((button) => {
        button.addEventListener("click", () => {
            const userId = Number(button.closest("[data-user-id]").dataset.userId);
            const selected = moderatorUsers.find((user) => user.id === userId);
            if (selected) {
                fillModeratorLicenseForm(selected);
            }
        });
    });
}

function compareModeratorLicenseUsers(left, right, sort) {
    if (sort === "name") {
        return String(left.fullName || left.username || "").localeCompare(String(right.fullName || right.username || ""), "ru");
    }
    if (sort === "newest") {
        return String(right.registrationDate || "").localeCompare(String(left.registrationDate || ""));
    }
    const priority = {PENDING: 0, REJECTED: 1, EXPIRED: 2, VERIFIED: 3};
    return (priority[left.docStatus] ?? 9) - (priority[right.docStatus] ?? 9)
            || String(right.registrationDate || "").localeCompare(String(left.registrationDate || ""));
}

function initModeratorLicenseControls() {
    document.getElementById("moderatorLicenseSearch")?.addEventListener("input", () => {
        renderModeratorUsers(moderatorUsers);
    });
    document.getElementById("moderatorLicenseSort")?.addEventListener("change", () => {
        renderModeratorUsers(moderatorUsers);
    });
    document.querySelectorAll("[data-moderator-license-tab]").forEach((button) => {
        button.addEventListener("click", () => {
            moderatorLicenseTab = button.dataset.moderatorLicenseTab || "queue";
            renderModeratorUsers(moderatorUsers);
        });
    });
}

async function loadModeratorPanel() {
    const panel = await pageRequest("/api/moderator/panel");
    const vehicleCards = await pageRequest("/api/vehicle-cards");
    const newsItems = await pageRequest("/api/news");
    moderatorUsers = panel.users || [];
    document.getElementById("candidateCountValue").textContent = String(panel.totalCandidates);
    document.getElementById("readyCountValue").textContent = String(panel.readyForApproval);
    document.getElementById("approvedCountValue").textContent = String(panel.alreadyApproved);
    document.getElementById("rejectedCountValue").textContent = String(
        moderatorUsers.filter((user) => user.docStatus === "REJECTED").length
    );
    renderModeratorUsers(moderatorUsers);
    renderModeratorSupportRequests(panel.supportRequests || []);
    renderPanelLongBookingStaffSection("moderator", panel, loadModeratorPanel);
    moderatorVehicleCardManager?.setCards(vehicleCards);
    moderatorNewsManager?.setNews(newsItems);
    if (moderatorUsers.length) {
        const active = moderatorUsers.find((user) => user.id === selectedModeratorUserId) || moderatorUsers[0];
        fillModeratorLicenseForm(active);
    } else {
        clearModeratorLicenseForm();
    }
}

async function initModeratorPage() {
    try {
        const sessionUser = await loadSessionUser();
        setupPageHeader(sessionUser);
        await loadModeratorPanel();
        clearPageError();
    } catch (error) {
        handleProtectedPageError(error, "Не удалось открыть панель модератора.");
    }
}

const moderatorTabs = ensureModeratorTabs();
if (moderatorTabs) {
    initPanelTabs(moderatorTabs);
}

document.getElementById("moderatorSupportRequestClear")?.addEventListener("click", async () => {
    await clearModeratorSupportRequests();
});

initModeratorLicenseControls();

const moderatorLicenseForm = document.getElementById("moderatorLicenseForm");
if (moderatorLicenseForm) {
    moderatorLicenseForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const status = document.getElementById("moderatorSaveStatus");
        const submitButton = document.getElementById("moderatorLicenseSubmit");
        if (!selectedModeratorUserId) {
            if (status) {
                status.textContent = "Сначала выберите пользователя.";
            }
            return;
        }

        const formData = new FormData(moderatorLicenseForm);
        const payload = {
            driverLicense: formData.get("driverLicense") || null,
            licenseExpiresAt: formData.get("licenseExpiresAt") || null,
            drivingBanUntil: formData.get("drivingBanUntil") || null,
            docStatus: formData.get("docStatus") || null
        };

        try {
            setButtonBusy(submitButton, true, "Сохранение...");
            if (status) {
                status.textContent = "";
            }
            await pageRequest(`/api/moderator/users/${encodeURIComponent(selectedModeratorUserId)}/license`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            if (status) {
                status.textContent = "Данные водительских прав сохранены.";
            }
            setModeratorFlash("Данные водительских прав сохранены.", "success");
            await loadModeratorPanel();
        } catch (error) {
            const message = extractErrorMessage(error, "Не удалось сохранить данные.");
            if (status) {
                status.textContent = message;
            }
            setModeratorFlash(message, "danger");
        } finally {
            setButtonBusy(submitButton, false, "Сохранить данные");
        }
    });
}

moderatorVehicleCardManager = initVehicleCardManager({
    prefix: "moderator",
    listId: "moderatorVehicleCardList",
    refresh: loadModeratorPanel
});

moderatorNewsManager = initNewsManager({
    prefix: "moderator",
    listId: "moderatorNewsList",
    refresh: loadModeratorPanel
});

initModeratorPage();
