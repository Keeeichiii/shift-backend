let supportPageUser = null;
let supportSuccessHideTimer = null;

function updateSupportPageAccess() {
    const hint = document.getElementById("supportPageHint");
    const form = document.getElementById("supportPageForm");
    const loginButton = document.getElementById("supportPageLoginBtn");

    if (!hint || !form || !loginButton) {
        return;
    }

    if (supportPageUser) {
        hint.textContent = `От имени: ${supportPageUser.username || supportPageUser.email || "пользователя"}.`;
        form.classList.remove("hidden");
        loginButton.classList.add("hidden");
    } else {
        hint.textContent = "Войдите в аккаунт, чтобы отправить обращение.";
        form.classList.add("hidden");
        loginButton.classList.remove("hidden");
    }
}

function setSupportSendingState(active) {
    document.querySelector(".support-form-card")?.classList.toggle("is-submitting", active);
    document.getElementById("supportPageSubmit")?.classList.toggle("is-loading", active);
}

function hideSupportSuccessAnimation() {
    const overlay = document.getElementById("supportSubmitSuccess");
    if (!overlay) {
        return;
    }
    overlay.classList.add("hidden");
    overlay.setAttribute("aria-hidden", "true");
}

function restartSupportSuccessAnimations(overlay) {
    overlay.querySelectorAll(
        ".support-submit-success__panel, .support-submit-success__circle, .support-submit-success__check, .support-submit-success__title, .support-submit-success__text, .support-submit-success__ripple"
    ).forEach((element) => {
        element.style.animation = "none";
        void element.offsetWidth;
        element.style.animation = "";
    });
}

function showSupportSuccessAnimation() {
    const overlay = document.getElementById("supportSubmitSuccess");
    if (!overlay) {
        return;
    }

    overlay.classList.remove("hidden");
    overlay.setAttribute("aria-hidden", "false");
    restartSupportSuccessAnimations(overlay);

    if (supportSuccessHideTimer) {
        clearTimeout(supportSuccessHideTimer);
    }
    supportSuccessHideTimer = window.setTimeout(() => {
        hideSupportSuccessAnimation();
        supportSuccessHideTimer = null;
    }, 5200);
}

function updateSupportContactPlaceholder() {
    const channel = document.getElementById("supportPageContactChannel");
    const value = document.getElementById("supportPageContactValue");
    if (!channel || !value) {
        return;
    }

    const placeholders = {
        phone: "+375 29 123-45-67",
        telegram: "@username или ID аккаунта",
        whatsapp: "+375 29 123-45-67",
        email: "name@example.com"
    };
    value.placeholder = placeholders[channel.value] || "Телефон, email или аккаунт";
}

async function initSupportPage() {
    try {
        supportPageUser = await loadSessionUser();
        setupPageHeader(supportPageUser);
        clearPageError();
    } catch (error) {
        if (error && (error.status === 401 || error.status === 403)) {
            supportPageUser = null;
            setupGuestPageHeader();
            clearPageError();
        } else {
            setPageError(extractErrorMessage(error, "Не удалось открыть страницу обращения."));
        }
    }

    if (!supportPageUser) {
        setupGuestPageHeader();
    }

    updateSupportPageAccess();
    updateSupportContactPlaceholder();
}

const supportPageLoginBtn = document.getElementById("supportPageLoginBtn");
if (supportPageLoginBtn) {
    supportPageLoginBtn.addEventListener("click", () => {
        window.location.href = "/index.html";
    });
}

const supportPageForm = document.getElementById("supportPageForm");
if (supportPageForm) {
    supportPageForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        const status = document.getElementById("supportPageStatus");
        const submitButton = document.getElementById("supportPageSubmit");

        if (!supportPageUser) {
            updateSupportPageAccess();
            return;
        }

        try {
            hideSupportSuccessAnimation();
            setButtonBusy(submitButton, true, "Отправка...");
            setSupportSendingState(true);
            if (status) {
                status.textContent = "";
            }

            const payload = Object.fromEntries(new FormData(supportPageForm).entries());
            await pageRequest("/api/support-requests", {
                method: "POST",
                body: JSON.stringify(payload)
            });

            supportPageForm.reset();
            updateSupportContactPlaceholder();
            showSupportSuccessAnimation();
            if (status) {
                status.textContent = "";
            }
        } catch (error) {
            hideSupportSuccessAnimation();
            if (status) {
                status.textContent = extractErrorMessage(error, "Не удалось отправить обращение.");
            }
        } finally {
            setSupportSendingState(false);
            setButtonBusy(submitButton, false, "Отправить обращение");
        }
    });
}

document.getElementById("supportPageContactChannel")?.addEventListener("change", updateSupportContactPlaceholder);

window.addEventListener("auth-state-changed", (event) => {
    supportPageUser = event.detail ? event.detail.user || null : null;
    updateSupportPageAccess();
});

initSupportPage();
