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

    return response.json();
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

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
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

function ensureGuestLoginButton() {
    const authBlock = document.querySelector(".auth-block");
    if (!authBlock) {
        return null;
    }
    let loginButton = document.getElementById("pageLoginBtn");
    if (!loginButton) {
        loginButton = document.createElement("button");
        loginButton.id = "pageLoginBtn";
        loginButton.type = "button";
        loginButton.className = "btn btn-small";
        loginButton.textContent = "Войти";
        authBlock.insertBefore(loginButton, authBlock.firstChild);
    }
    if (loginButton.dataset.bound !== "true") {
        loginButton.addEventListener("click", () => {
            window.location.href = "/index.html";
        });
        loginButton.dataset.bound = "true";
    }
    return loginButton;
}

function ensureGuestRegisterButton() {
    const authBlock = document.querySelector(".auth-block");
    if (!authBlock) {
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
        if (loginButton) {
            loginButton.insertAdjacentElement("afterend", registerButton);
        } else {
            authBlock.insertBefore(registerButton, authBlock.firstChild);
        }
    }
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
    const userLabel = document.getElementById("pageUserLabel");
    const roleLabel = document.getElementById("pageRoleLabel");
    const logoutButton = document.getElementById("pageLogoutBtn");
    const panelLink = document.getElementById("panelLink");
    const loginButton = ensureGuestLoginButton();

    if (userLabel) {
        userLabel.textContent = user.username || user.email || "Пользователь";
        userLabel.classList.remove("hidden");
    }
    if (roleLabel) {
        roleLabel.textContent = Array.isArray(user.roles) ? user.roles.join(", ") : "";
    }
    if (logoutButton) {
        logoutButton.classList.remove("hidden");
        logoutButton.addEventListener("click", async () => {
            setButtonBusy(logoutButton, true, "Выход...");
            await performLogout();
        });
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
    const userLabel = document.getElementById("pageUserLabel");
    const roleLabel = document.getElementById("pageRoleLabel");
    const logoutButton = document.getElementById("pageLogoutBtn");
    const loginButton = ensureGuestLoginButton();
    const registerButton = ensureGuestRegisterButton();

    if (panelLink) panelLink.classList.add("hidden");
    if (userLabel) userLabel.classList.add("hidden");
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
