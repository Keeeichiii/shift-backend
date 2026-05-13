function renderRoleBadges(roles) {
    return roles.map((role) => `<span class="badge badge_role">${escapeHtml(role)}</span>`).join("");
}

function statusClass(status) {
    return `badge_status_${String(status || "").toLowerCase()}`;
}

let adminUsers = [];
let selectedAdminUserId = null;
let adminVehicleCardManager = null;
let adminNewsManager = null;

function renderAdminSupportRequests(requests) {
    const list = document.getElementById("adminSupportRequestList");
    if (!list) {
        return;
    }

    if (!requests.length) {
        list.innerHTML = `<div class="support-message-card__empty"><strong>Сообщений пока нет.</strong><p>Новые обращения из формы техподдержки появятся здесь.</p></div>`;
        return;
    }

    list.innerHTML = requests.map((request) => `
        <article class="support-message-card">
            <div class="support-message-card__head">
                <div>
                    <strong>${escapeHtml(request.subject)}</strong>
                    <p>${escapeHtml(request.fullName)}${request.username ? ` (@${escapeHtml(request.username)})` : ""}</p>
                </div>
                <div class="support-message-card__meta">
                    <span class="badge badge_role">${escapeHtml(request.contactChannel)}</span>
                    <span class="badge badge_role">${escapeHtml(formatDateTime(request.createdAt))}</span>
                </div>
            </div>
            <div class="support-message-card__body">
                <p>${escapeHtml(request.email || "Email не указан")}</p>
                <p>${escapeHtml(request.message)}</p>
            </div>
        </article>
    `).join("");
}

function renderLicenseImages(containerId, user) {
    const container = document.getElementById(containerId);
    const items = [];

    if (user?.licenseFrontImage) {
        items.push(`
            <figure class="license-shot">
                <img src="${user.licenseFrontImage}" alt="Лицевая сторона прав">
                <figcaption>Лицевая сторона</figcaption>
            </figure>
        `);
    }
    if (user?.licenseBackImage) {
        items.push(`
            <figure class="license-shot">
                <img src="${user.licenseBackImage}" alt="Обратная сторона прав">
                <figcaption>Обратная сторона</figcaption>
            </figure>
        `);
    }
    if (user?.passportMainImage) {
        items.push(`
            <figure class="license-shot">
                <img src="${user.passportMainImage}" alt="Главная страница паспорта">
                <figcaption>Главная страница паспорта</figcaption>
            </figure>
        `);
    }

    container.innerHTML = items.length
        ? items.join("")
        : `<div class="info-item"><span>Нет изображений</span><strong>Пользователь ещё не загрузил права и паспорт.</strong></div>`;
}

function fillAdminForm(user) {
    selectedAdminUserId = user.id;
    document.getElementById("adminSelectedUser").value = `${user.fullName} (@${user.username})`;
    document.getElementById("adminDriverLicense").value = user.driverLicense || "";
    document.getElementById("adminLicenseExpiresAt").value = user.licenseExpiresAt || "";
    document.getElementById("adminDrivingBanUntil").value = user.drivingBanUntil || "";
    document.getElementById("adminDocStatus").value = user.docStatus;
    renderLicenseImages("adminLicensePreview", user);
}

function renderAdminUsers(users) {
    const list = document.getElementById("adminUserList");
    if (!users.length) {
        list.innerHTML = `<div class="admin-user-card"><strong>Пользователей пока нет</strong><p>После регистрации данные появятся здесь.</p></div>`;
        return;
    }

    list.innerHTML = users.map((user) => `
        <div class="admin-user-card" data-user-id="${escapeHtml(user.id)}">
            <strong>${escapeHtml(user.fullName)} <small>@${escapeHtml(user.username)}</small></strong>
            <p>${escapeHtml(user.email || "Email не указан")}</p>
            <div class="role-badges">${renderRoleBadges(user.roles)}</div>
            <div class="meta-badges">
                <span class="badge ${statusClass(user.docStatus)}">${escapeHtml(user.docStatus)}</span>
                <span class="badge badge_role">Права до ${escapeHtml(formatDate(user.licenseExpiresAt))}</span>
                <span class="badge badge_role">Лишение до ${escapeHtml(formatDate(user.drivingBanUntil))}</span>
            </div>
            <p>${escapeHtml(user.moderationNote)}</p>
            <div class="panel-actions">
                <button class="btn btn-small select-admin-user" type="button">Редактировать</button>
            </div>
        </div>
    `).join("");

    list.querySelectorAll(".select-admin-user").forEach((button) => {
        button.addEventListener("click", () => {
            const userId = Number(button.closest("[data-user-id]").dataset.userId);
            const selected = adminUsers.find((user) => user.id === userId);
            if (selected) {
                fillAdminForm(selected);
            }
        });
    });
}

async function loadAdminPage() {
    try {
        const sessionUser = await loadSessionUser();
        setupPageHeader(sessionUser);
        const panel = await pageRequest("/api/admin/panel");
        const vehicleCards = await pageRequest("/api/vehicle-cards");
        const newsItems = await pageRequest("/api/news");
        adminUsers = panel.users;

        document.getElementById("totalUsersValue").textContent = String(panel.totalUsers);
        document.getElementById("activeUsersValue").textContent = String(panel.activeUsers);
        document.getElementById("pendingUsersValue").textContent = String(panel.pendingModeration);
        document.getElementById("approvedUsersValue").textContent = String(panel.approvedUsers);
        renderAdminUsers(panel.users);
        renderAdminSupportRequests(panel.supportRequests || []);
        renderPanelLongBookingStaffSection("admin", panel, loadAdminPage);
        adminVehicleCardManager?.setCards(vehicleCards);
        adminNewsManager?.setNews(newsItems);
        if (panel.users.length) {
            const active = adminUsers.find((user) => user.id === selectedAdminUserId) || panel.users[0];
            fillAdminForm(active);
        } else {
            renderLicenseImages("adminLicensePreview", null);
        }
        clearPageError();
    } catch (error) {
        handleProtectedPageError(error, "Не удалось открыть админ-панель.");
    }
}

const adminLicenseForm = document.getElementById("adminLicenseForm");
if (adminLicenseForm) {
    adminLicenseForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const status = document.getElementById("adminSaveStatus");
        if (!selectedAdminUserId) {
            status.textContent = "Сначала выберите пользователя.";
            return;
        }

        const formData = new FormData(adminLicenseForm);
        const payload = {
            driverLicense: formData.get("driverLicense") || null,
            licenseExpiresAt: formData.get("licenseExpiresAt") || null,
            drivingBanUntil: formData.get("drivingBanUntil") || null,
            docStatus: formData.get("docStatus") || null
        };
        const submitButton = adminLicenseForm.querySelector('button[type="submit"]');

        try {
            setButtonBusy(submitButton, true, "Сохранение...");
            status.textContent = "";
            await pageRequest(`/api/users/${selectedAdminUserId}/license`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            status.textContent = "Данные водительских прав сохранены.";
            await loadAdminPage();
        } catch (error) {
            status.textContent = extractErrorMessage(error, "Не удалось сохранить данные.");
        } finally {
            setButtonBusy(submitButton, false, "Сохранение...");
        }
    });
}

const adminTabs = document.querySelector('[data-panel-tabs="admin"]');
if (adminTabs) {
    initPanelTabs(adminTabs);
}

adminVehicleCardManager = initVehicleCardManager({
    prefix: "admin",
    listId: "adminVehicleCardList",
    refresh: loadAdminPage
});

adminNewsManager = initNewsManager({
    prefix: "admin",
    listId: "adminNewsList",
    refresh: loadAdminPage
});

loadAdminPage();
