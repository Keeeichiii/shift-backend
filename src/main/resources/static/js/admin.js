function renderRoleBadges(roles) {
    return roles.map((role) => `<span class="badge badge_role">${escapeHtml(role)}</span>`).join("");
}

function supportContactChannelLabel(channel) {
    const labels = {
        phone: "Телефон",
        telegram: "Telegram",
        whatsapp: "WhatsApp",
        email: "Email"
    };
    return labels[channel] || channel || "Не указан";
}

function statusClass(status) {
    return `badge_status_${String(status || "").toLowerCase()}`;
}

let adminUsers = [];
let adminModerationUsers = [];
let selectedManagedUserId = null;
let selectedAdminUserId = null;
let adminLicenseTab = "queue";
let adminVehicleCardManager = null;
let adminNewsManager = null;

function setAdminSupportEmptyState() {
    const list = document.getElementById("adminSupportRequestList");
    if (!list) {
        return;
    }
    list.innerHTML = `<div class="support-message-card__empty"><strong>Сообщений пока нет.</strong><p>Новые обращения из формы техподдержки появятся здесь.</p></div>`;
}

function renderAdminSupportRequests(requests) {
    const list = document.getElementById("adminSupportRequestList");
    if (!list) {
        return;
    }

    if (!requests.length) {
        setAdminSupportEmptyState();
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
                <button type="button" class="btn btn-small btn-secondary admin-support-delete-btn">Удалить сообщение</button>
            </div>
        </article>
    `).join("");

    list.querySelectorAll(".admin-support-delete-btn").forEach((button) => {
        button.addEventListener("click", async () => {
            const requestId = button.closest("[data-support-request-id]")?.dataset.supportRequestId;
            if (!requestId) {
                return;
            }
            await deleteAdminSupportRequest(requestId, button);
        });
    });
}

async function deleteAdminSupportRequest(requestId, button) {
    if (!window.confirm("Удалить это сообщение?")) {
        return;
    }
    const status = document.getElementById("adminSupportRequestStatus");
    try {
        setButtonBusy(button, true, "Удаление...");
        if (status) {
            status.textContent = "";
        }
        await pageRequest(`/api/support-requests/${encodeURIComponent(requestId)}`, {method: "DELETE"});
        button.closest("[data-support-request-id]")?.remove();
        if (!document.querySelector("#adminSupportRequestList [data-support-request-id]")) {
            setAdminSupportEmptyState();
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

async function clearAdminSupportRequests(button = document.getElementById("adminSupportRequestClear")) {
    if (!window.confirm("Очистить все сообщения?")) {
        return;
    }
    const status = document.getElementById("adminSupportRequestStatus");
    try {
        setButtonBusy(button, true, "Очистка...");
        if (status) {
            status.textContent = "";
        }
        await pageRequest("/api/support-requests", {method: "DELETE"});
        setAdminSupportEmptyState();
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

function fillAdminForm(user) {
    selectedAdminUserId = user.id;
    document.getElementById("adminSelectedUser").value = `${user.fullName} (@${user.username})`;
    document.getElementById("adminDriverLicense").value = user.driverLicense || "";
    document.getElementById("adminLicenseExpiresAt").value = user.licenseExpiresAt || "";
    document.getElementById("adminDrivingBanUntil").value = user.drivingBanUntil || "";
    document.getElementById("adminDocStatus").value = user.docStatus;
    renderLicenseImages("adminLicensePreview", user);
}

function activateAdminTab(targetId) {
    const tabsRoot = document.querySelector('[data-panel-tabs="admin"]');
    if (!tabsRoot) {
        return;
    }

    tabsRoot.querySelectorAll(".panel-tab").forEach((tab) => {
        tab.classList.toggle("is-active", tab.dataset.panelTarget === targetId);
    });
    document.querySelectorAll(".panel-section").forEach((section) => {
        section.classList.toggle("hidden", section.id !== targetId);
    });
}

function setAdminUserStatus(message = "") {
    const status = document.getElementById("adminUserStatus");
    if (status) {
        status.textContent = message;
    }
}

function fillAdminUserForm(user) {
    selectedManagedUserId = user.id;
    document.getElementById("adminUserId").value = user.id || "";
    document.getElementById("adminUserUsername").value = user.username || "";
    document.getElementById("adminUserEmail").value = user.email || "";
    document.getElementById("adminUserPassword").value = "";
    document.getElementById("adminUserRegionId").value = user.regionId || "";
    document.getElementById("adminUserFirstName").value = user.firstName || "";
    document.getElementById("adminUserLastName").value = user.lastName || "";
    document.getElementById("adminUserProfileName").value = user.profileName || "";
    document.getElementById("adminUserPhone").value = user.phone || "";
    document.getElementById("adminUserDriverLicense").value = user.driverLicense || "";
    document.getElementById("adminUserLicenseExpiresAt").value = user.licenseExpiresAt || "";
    document.getElementById("adminUserDrivingBanUntil").value = user.drivingBanUntil || "";
    document.getElementById("adminUserDocStatus").value = user.docStatus || "";
    document.getElementById("adminUserBio").value = user.bio || "";
    const roles = new Set(Array.isArray(user.roles) ? user.roles : []);
    document.querySelectorAll('#adminUserForm input[name="roles"]').forEach((input) => {
        input.checked = roles.has(input.value);
    });
    document.getElementById("adminUserDelete").classList.remove("hidden");
    setAdminUserStatus("");
}

function clearAdminUserForm() {
    selectedManagedUserId = null;
    document.getElementById("adminUserForm")?.reset();
    document.getElementById("adminUserId").value = "";
    document.getElementById("adminUserDelete").classList.add("hidden");
}

function buildNullableText(value) {
    const trimmed = String(value ?? "").trim();
    return trimmed || null;
}

async function selectAdminUserForEditing(userId, activeButton = null) {
    try {
        setButtonBusy(activeButton, true, "Загрузка...");
        setAdminUserStatus("Загрузка пользователя...");
        const user = await pageRequest(`/api/users/${encodeURIComponent(userId)}`);
        const panelUser = adminUsers.find((item) => item.id === userId) || {};
        fillAdminUserForm({...panelUser, ...user, email: panelUser.email, roles: panelUser.roles});
        document.getElementById("adminUserManageCard")?.scrollIntoView({behavior: "smooth", block: "start"});
    } catch (error) {
        setAdminUserStatus(extractErrorMessage(error, "Не удалось загрузить пользователя."));
    } finally {
        setButtonBusy(activeButton, false, "Редактировать");
    }
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
                <span class="badge ${statusClass(user.docStatus)}">${escapeHtml(documentStatusLabel(user.docStatus))}</span>
                <span class="badge badge_role">Права до ${escapeHtml(formatDate(user.licenseExpiresAt))}</span>
                <span class="badge badge_role">Лишение до ${escapeHtml(formatDate(user.drivingBanUntil))}</span>
            </div>
            <p>${escapeHtml(user.moderationNote)}</p>
            <div class="panel-actions">
                <button class="btn btn-small select-admin-user" type="button">Редактировать</button>
                <button class="btn btn-small btn-danger delete-admin-user" type="button">Удалить</button>
            </div>
        </div>
    `).join("");

    list.querySelectorAll(".select-admin-user").forEach((button) => {
        button.addEventListener("click", async () => {
            const userId = Number(button.closest("[data-user-id]").dataset.userId);
            await selectAdminUserForEditing(userId, button);
        });
    });

    list.querySelectorAll(".delete-admin-user").forEach((button) => {
        button.addEventListener("click", async () => {
            const userId = Number(button.closest("[data-user-id]").dataset.userId);
            await deleteAdminUser(userId, button);
        });
    });
}

function isRegularPanelUser(user) {
    const roles = Array.isArray(user.roles) ? user.roles : [];
    return roles.includes("USER") && !roles.includes("ADMIN") && !roles.includes("MODERATOR");
}

function renderAdminLicenseUsers(users) {
    const list = document.getElementById("adminLicenseUserList");
    if (!list) {
        return;
    }

    const query = String(document.getElementById("adminLicenseSearch")?.value || "").trim().toLowerCase();
    const sort = document.getElementById("adminLicenseSort")?.value || "priority";
    const visibleUsers = users
        .filter((user) => adminLicenseTab === "approved" ? user.docStatus === "VERIFIED" : user.docStatus !== "VERIFIED")
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
        .sort((left, right) => compareAdminLicenseUsers(left, right, sort));

    document.querySelectorAll("[data-admin-license-tab]").forEach((button) => {
        button.classList.toggle("is-active", button.dataset.adminLicenseTab === adminLicenseTab);
        const count = button.dataset.adminLicenseTab === "approved"
                ? users.filter((user) => user.docStatus === "VERIFIED").length
                : users.filter((user) => user.docStatus !== "VERIFIED").length;
        button.textContent = `${button.dataset.adminLicenseTab === "approved" ? "Одобренные" : "На проверку"} (${count})`;
    });

    if (!visibleUsers.length) {
        const emptyText = adminLicenseTab === "approved"
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
                <span class="badge ${statusClass(user.docStatus)}">${escapeHtml(documentStatusLabel(user.docStatus))}</span>
                <span class="badge badge_role">Права до ${escapeHtml(formatDate(user.licenseExpiresAt))}</span>
                <span class="badge badge_role">Лишение до ${escapeHtml(formatDate(user.drivingBanUntil))}</span>
            </div>
            <p>${escapeHtml(user.moderationNote)}</p>
            <div class="panel-actions">
                <button class="btn btn-small select-admin-license-user" type="button">Выбрать</button>
            </div>
        </div>
    `).join("");

    list.querySelectorAll(".select-admin-license-user").forEach((button) => {
        button.addEventListener("click", () => {
            const userId = Number(button.closest("[data-user-id]").dataset.userId);
            const selected = adminModerationUsers.find((user) => user.id === userId);
            if (selected) {
                fillAdminForm(selected);
            }
        });
    });
}

function compareAdminLicenseUsers(left, right, sort) {
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

function initAdminLicenseControls() {
    document.getElementById("adminLicenseSearch")?.addEventListener("input", () => {
        renderAdminLicenseUsers(adminModerationUsers);
    });
    document.getElementById("adminLicenseSort")?.addEventListener("change", () => {
        renderAdminLicenseUsers(adminModerationUsers);
    });
    document.querySelectorAll("[data-admin-license-tab]").forEach((button) => {
        button.addEventListener("click", () => {
            adminLicenseTab = button.dataset.adminLicenseTab || "queue";
            renderAdminLicenseUsers(adminModerationUsers);
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
        adminModerationUsers = panel.users.filter(isRegularPanelUser);

        document.getElementById("totalUsersValue").textContent = String(panel.totalUsers);
        document.getElementById("activeUsersValue").textContent = String(panel.activeUsers);
        document.getElementById("pendingUsersValue").textContent = String(panel.pendingModeration);
        document.getElementById("approvedUsersValue").textContent = String(panel.approvedUsers);
        renderAdminUsers(panel.users);
        renderAdminLicenseUsers(adminModerationUsers);
        renderAdminSupportRequests(panel.supportRequests || []);
        renderPanelLongBookingStaffSection("admin", panel, loadAdminPage);
        adminVehicleCardManager?.setCards(vehicleCards);
        adminNewsManager?.setNews(newsItems);
        if (!adminUsers.length) {
            clearAdminUserForm();
        } else if (selectedManagedUserId && !adminUsers.some((user) => user.id === selectedManagedUserId)) {
            clearAdminUserForm();
        }
        if (adminModerationUsers.length) {
            const active = adminModerationUsers.find((user) => user.id === selectedAdminUserId) || adminModerationUsers[0];
            fillAdminForm(active);
        } else {
            selectedAdminUserId = null;
            document.getElementById("adminSelectedUser").value = "";
            document.getElementById("adminDriverLicense").value = "";
            document.getElementById("adminLicenseExpiresAt").value = "";
            document.getElementById("adminDrivingBanUntil").value = "";
            document.getElementById("adminDocStatus").value = "PENDING";
            renderLicenseImages("adminLicensePreview", null);
        }
        clearPageError();
    } catch (error) {
        handleProtectedPageError(error, "Не удалось открыть админ-панель.");
    }
}

async function deleteAdminUser(userId, activeButton = document.getElementById("adminUserDelete")) {
    const user = adminUsers.find((item) => item.id === userId);
    const label = user ? `${user.fullName} (@${user.username})` : `#${userId}`;
    if (!window.confirm(`Удалить пользователя ${label}? Это действие нельзя отменить.`)) {
        return;
    }

    try {
        setButtonBusy(activeButton, true, "Удаление...");
        setAdminUserStatus("");
        await pageRequest(`/api/users/${encodeURIComponent(userId)}`, {method: "DELETE"});
        if (selectedManagedUserId === userId) {
            selectedManagedUserId = null;
        }
        if (selectedAdminUserId === userId) {
            selectedAdminUserId = null;
        }
        await loadAdminPage();
    } catch (error) {
        setAdminUserStatus(extractErrorMessage(error, "Не удалось удалить пользователя."));
    } finally {
        setButtonBusy(activeButton, false, "Удалить");
    }
}

const adminUserForm = document.getElementById("adminUserForm");
if (adminUserForm) {
    adminUserForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!selectedManagedUserId) {
            setAdminUserStatus("Сначала выберите пользователя.");
            return;
        }

        const formData = new FormData(adminUserForm);
        const regionValue = buildNullableText(formData.get("regionId"));
        const password = buildNullableText(formData.get("password"));
        const payload = {
            username: buildNullableText(formData.get("username")),
            email: buildNullableText(formData.get("email")),
            password,
            roles: formData.getAll("roles"),
            regionId: regionValue === null ? null : Number(regionValue),
            firstName: buildNullableText(formData.get("firstName")),
            lastName: buildNullableText(formData.get("lastName")),
            profileName: buildNullableText(formData.get("profileName")),
            bio: buildNullableText(formData.get("bio")),
            phone: buildNullableText(formData.get("phone")),
            driverLicense: buildNullableText(formData.get("driverLicense")),
            licenseExpiresAt: buildNullableText(formData.get("licenseExpiresAt")),
            drivingBanUntil: buildNullableText(formData.get("drivingBanUntil")),
            docStatus: buildNullableText(formData.get("docStatus"))
        };
        const submitButton = document.getElementById("adminUserSubmit");

        try {
            setButtonBusy(submitButton, true, "Сохранение...");
            setAdminUserStatus("");
            await pageRequest(`/api/users/${encodeURIComponent(selectedManagedUserId)}/management`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            setAdminUserStatus("Пользователь сохранён.");
            await loadAdminPage();
        } catch (error) {
            setAdminUserStatus(extractErrorMessage(error, "Не удалось сохранить пользователя."));
        } finally {
            setButtonBusy(submitButton, false, "Сохранить пользователя");
        }
    });
}

const adminUserDelete = document.getElementById("adminUserDelete");
if (adminUserDelete) {
    adminUserDelete.addEventListener("click", async () => {
        if (!selectedManagedUserId) {
            setAdminUserStatus("Сначала выберите пользователя.");
            return;
        }
        await deleteAdminUser(selectedManagedUserId, adminUserDelete);
    });
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

document.getElementById("adminSupportRequestClear")?.addEventListener("click", async () => {
    await clearAdminSupportRequests();
});

initAdminLicenseControls();

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
