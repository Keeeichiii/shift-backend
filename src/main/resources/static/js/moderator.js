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

function renderModeratorSupportRequests(requests) {
    const list = document.getElementById("moderatorSupportRequestList");
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

function renderModeratorUsers(users) {
    const list = document.getElementById("reviewList");
    if (!users.length) {
        list.innerHTML = `<div class="review-card"><strong>Очередь пуста</strong><p>Все пользователи уже обработаны.</p></div>`;
        return;
    }

    list.innerHTML = users.map((user) => `
        <div class="review-card" data-user-id="${escapeHtml(user.id)}">
            <strong>${escapeHtml(user.fullName)} <small>@${escapeHtml(user.username)}</small></strong>
            <p>${escapeHtml(user.email || "Email не указан")}</p>
            <div class="meta-badges">
                <span class="badge badge_role">Права до ${escapeHtml(formatDate(user.licenseExpiresAt))}</span>
                <span class="badge badge_role">Лишение до ${escapeHtml(formatDate(user.drivingBanUntil))}</span>
                <span class="badge badge_status_${String(user.docStatus).toLowerCase()}">${escapeHtml(user.docStatus)}</span>
            </div>
            <div class="license-preview-grid">
                ${user.licenseFrontImage ? `
                    <figure class="license-shot">
                        <img src="${user.licenseFrontImage}" alt="Лицевая сторона прав">
                        <figcaption>Лицевая сторона</figcaption>
                    </figure>
                ` : ""}
                ${user.licenseBackImage ? `
                    <figure class="license-shot">
                        <img src="${user.licenseBackImage}" alt="Обратная сторона прав">
                        <figcaption>Обратная сторона</figcaption>
                    </figure>
                ` : ""}
                ${user.passportMainImage ? `
                    <figure class="license-shot">
                        <img src="${user.passportMainImage}" alt="Главная страница паспорта">
                        <figcaption>Главная страница паспорта</figcaption>
                    </figure>
                ` : ""}
            </div>
            <p>${escapeHtml(user.moderationNote)}</p>
            <div class="review-actions">
                <button class="btn btn-small approve-btn" type="button" ${user.eligibleForApproval ? "" : "disabled"}>Одобрить</button>
                <button class="btn btn-small btn-danger reject-btn" type="button">Отклонить</button>
            </div>
        </div>
    `).join("");

    list.querySelectorAll(".approve-btn").forEach((button) => {
        button.addEventListener("click", async () => {
            const card = button.closest("[data-user-id]");
            await moderateUser(card.dataset.userId, "approve");
        });
    });

    list.querySelectorAll(".reject-btn").forEach((button) => {
        button.addEventListener("click", async () => {
            const card = button.closest("[data-user-id]");
            await moderateUser(card.dataset.userId, "reject");
        });
    });
}

async function moderateUser(userId, action) {
    const activeButton = document.querySelector(`[data-user-id="${userId}"] .${action === "approve" ? "approve-btn" : "reject-btn"}`);
    try {
        setButtonBusy(activeButton, true, action === "approve" ? "Одобрение..." : "Отклонение...");
        await pageRequest(`/api/moderator/users/${userId}/${action}`, {method: "POST"});
        setModeratorFlash(
            action === "approve" ? "Пользователь одобрен." : "Пользователь отклонён.",
            action === "approve" ? "success" : "danger"
        );
        await loadModeratorPanel();
    } catch (error) {
        setModeratorFlash(extractErrorMessage(error, "Не удалось выполнить действие модерации."), "danger");
    } finally {
        setButtonBusy(activeButton, false, action === "approve" ? "Одобрение..." : "Отклонение...");
    }
}

async function loadModeratorPanel() {
    const panel = await pageRequest("/api/moderator/panel");
    const vehicleCards = await pageRequest("/api/vehicle-cards");
    const newsItems = await pageRequest("/api/news");
    document.getElementById("candidateCountValue").textContent = String(panel.totalCandidates);
    document.getElementById("readyCountValue").textContent = String(panel.readyForApproval);
    document.getElementById("approvedCountValue").textContent = String(panel.alreadyApproved);
    document.getElementById("rejectedCountValue").textContent = String(
        panel.users.filter((user) => user.docStatus === "REJECTED").length
    );
    renderModeratorUsers(panel.users);
    renderModeratorSupportRequests(panel.supportRequests || []);
    renderPanelLongBookingStaffSection("moderator", panel, loadModeratorPanel);
    moderatorVehicleCardManager?.setCards(vehicleCards);
    moderatorNewsManager?.setNews(newsItems);
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

const moderatorTabs = document.querySelector('[data-panel-tabs="moderator"]');
if (moderatorTabs) {
    initPanelTabs(moderatorTabs);
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
