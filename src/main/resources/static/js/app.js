const loginModal = document.getElementById("loginModal");
const registerModal = document.getElementById("registerModal");
const openLogin = document.getElementById("openLogin");
const openRegister = document.getElementById("openRegister");
const closeLogin = document.getElementById("closeLogin");
const closeRegister = document.getElementById("closeRegister");
const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");
const logoutBtn = document.getElementById("logoutBtn");
const currentUserLabel = document.getElementById("currentUserLabel");
const loginStatus = document.getElementById("loginStatus");
const registerStatus = document.getElementById("registerStatus");
const panelLink = document.getElementById("panelLink");

function emitAuthStateChanged(user) {
    window.dispatchEvent(new CustomEvent("auth-state-changed", {
        detail: {user: user || null}
    }));
}

function syncIndexHeaderAuthButtons(event) {
    const openL = document.getElementById("openLogin");
    const openR = document.getElementById("openRegister");
    const logoutB = document.getElementById("logoutBtn");
    if (!openL || !openR || !logoutB) {
        return;
    }
    const user = event?.detail?.user ?? null;
    if (user) {
        openL.classList.add("hidden");
        openR.classList.add("hidden");
        logoutB.classList.remove("hidden");
    } else {
        openL.classList.remove("hidden");
        openR.classList.remove("hidden");
        logoutB.classList.add("hidden");
    }
}

window.addEventListener("auth-state-changed", syncIndexHeaderAuthButtons);

function maybeOpenRegisterFromQuery() {
    if (!registerModal) {
        return;
    }
    const params = new URLSearchParams(window.location.search);
    if (params.get("register") !== "1") {
        return;
    }
    if (openLogin && openLogin.classList.contains("hidden")) {
        window.history.replaceState({}, "", window.location.pathname + window.location.hash);
        return;
    }
    openModal(registerModal);
    window.history.replaceState({}, "", window.location.pathname + window.location.hash);
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

function openModal(modal) {
    if (!modal) return;
    modal.classList.remove("hidden");
}

function closeModal(modal) {
    if (!modal) return;
    modal.classList.add("hidden");
}

function closeAllModals() {
    closeModal(loginModal);
    closeModal(registerModal);
}

if (openLogin && loginModal) {
    openLogin.addEventListener("click", () => openModal(loginModal));
}

if (openRegister && registerModal) {
    openRegister.addEventListener("click", () => openModal(registerModal));
}

if (closeLogin && loginModal) {
    closeLogin.addEventListener("click", () => closeModal(loginModal));
}

if (closeRegister && registerModal) {
    closeRegister.addEventListener("click", () => closeModal(registerModal));
}

[loginModal, registerModal].forEach((modal) => {
    if (!modal) return;
    modal.addEventListener("click", (event) => {
        if (event.target === modal) {
            closeModal(modal);
        }
    });
});

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        closeAllModals();
    }
});

function extractErrorMessage(error, fallback) {
    if (!error || !error.message) {
        return fallback;
    }
    return error.message.replace(/^"|"$/g, "") || fallback;
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }
    });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Request failed");
    }
    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function updateRolePanel(user) {
    if (!panelLink) return;
    panelLink.classList.add("hidden");
    if (!user || !Array.isArray(user.roles)) return;

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

function setLoggedInState(user) {
    if (openLogin) openLogin.classList.add("hidden");
    if (openRegister) openRegister.classList.add("hidden");
    if (logoutBtn) logoutBtn.classList.remove("hidden");
    if (currentUserLabel) {
        currentUserLabel.textContent = user.username;
        currentUserLabel.classList.remove("hidden");
    }
    if (loginStatus) loginStatus.textContent = `Вы вошли как ${user.username} (${user.roles.join(", ")})`;
    if (registerStatus) registerStatus.textContent = "";
    updateRolePanel(user);
    emitAuthStateChanged(user);
}

function setLoggedOutState() {
    if (openLogin) openLogin.classList.remove("hidden");
    if (openRegister) openRegister.classList.remove("hidden");
    if (logoutBtn) logoutBtn.classList.add("hidden");
    if (currentUserLabel) {
        currentUserLabel.textContent = "";
        currentUserLabel.classList.add("hidden");
    }
    if (loginStatus) loginStatus.textContent = "";
    if (registerStatus) registerStatus.textContent = "";
    if (panelLink) panelLink.classList.add("hidden");
    emitAuthStateChanged(null);
}

async function loadSession() {
    try {
        const user = await request("/api/auth/me");
        setLoggedInState(user);
    } catch {
        setLoggedOutState();
    } finally {
        maybeOpenRegisterFromQuery();
    }
}

if (loginForm) {
    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = Object.fromEntries(new FormData(loginForm).entries());
        const submitButton = loginForm.querySelector('button[type="submit"]');
        try {
            setButtonBusy(submitButton, true, "Вход...");
            if (loginStatus) loginStatus.textContent = "";
            const user = await request("/api/auth/login", {
                method: "POST",
                body: JSON.stringify(data)
            });
            setLoggedInState(user);
            closeModal(loginModal);
            loginForm.reset();
        } catch (error) {
            loginStatus.textContent = extractErrorMessage(error, "Ошибка входа: проверьте email и пароль.");
        } finally {
            setButtonBusy(submitButton, false, "Вход...");
        }
    });
}

if (registerForm) {
    registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const data = Object.fromEntries(new FormData(registerForm).entries());
        const submitButton = registerForm.querySelector('button[type="submit"]');
        try {
            setButtonBusy(submitButton, true, "Регистрация...");
            if (registerStatus) registerStatus.textContent = "";
            await request("/api/auth/register", {
                method: "POST",
                body: JSON.stringify(data)
            });
            registerStatus.textContent = "Регистрация успешна. Войдите в аккаунт и загрузите документы в личном кабинете.";
            registerForm.reset();
        } catch (error) {
            registerStatus.textContent = extractErrorMessage(error, "Ошибка регистрации. Проверьте данные.");
        } finally {
            setButtonBusy(submitButton, false, "Регистрация...");
        }
    });
}

if (logoutBtn) {
    logoutBtn.addEventListener("click", async () => {
        setButtonBusy(logoutBtn, true, "Выход...");
        try {
            await request("/api/auth/logout", {method: "POST"});
        } finally {
            setLoggedOutState();
            window.location.replace("/index.html");
        }
    });
}

loadSession();
