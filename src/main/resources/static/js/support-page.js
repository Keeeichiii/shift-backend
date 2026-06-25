let supportPageUser = null;
let supportSuccessHideTimer = null;

function supportChannelConfig(channel) {
    const config = {
        phone: {
            placeholder: "+375 29 123-45-67",
            inputMode: "tel",
            type: "tel"
        },
        telegram: {
            placeholder: "@username или ID аккаунта",
            inputMode: "text",
            type: "text"
        },
        whatsapp: {
            placeholder: "+375 29 123-45-67",
            inputMode: "tel",
            type: "tel"
        },
        email: {
            placeholder: "name@example.com",
            inputMode: "email",
            type: "email"
        }
    };
    return config[channel] || config.phone;
}

function normalizeSupportContactValue(channel, value) {
    const trimmed = String(value || "").trim();
    if (channel === "phone" || channel === "whatsapp") {
        return trimmed.replace(/[^\d+\s()-]/g, "");
    }
    return trimmed;
}

function validateSupportFormData(channel, contactValue, subject, message) {
    const normalizedChannel = String(channel || "").trim();
    const normalizedContact = normalizeSupportContactValue(normalizedChannel, contactValue);
    const normalizedSubject = String(subject || "").trim();
    const normalizedMessage = String(message || "").trim();
    const allowedChannels = ["phone", "telegram", "whatsapp", "email"];
    if (!allowedChannels.includes(normalizedChannel)) {
        return "Выберите корректный канал для ответа.";
    }
    if (!normalizedSubject) {
        return "Укажите тему обращения.";
    }
    if (!normalizedMessage) {
        return "Введите сообщение.";
    }
    if (normalizedChannel === "phone" || normalizedChannel === "whatsapp") {
        const digits = normalizedContact.replace(/\D/g, "");
        if (!/^\+?[\d\s()-]+$/.test(normalizedContact) || digits.length < 7 || digits.length > 15) {
            return "Для телефона и WhatsApp используйте только цифры и телефонные символы.";
        }
    }
    if (normalizedChannel === "telegram" && !/^@?[A-Za-z0-9_]{5,32}$/.test(normalizedContact)) {
        return "Укажите корректный Telegram: @username или ID без пробелов.";
    }
    if (normalizedChannel === "email" && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedContact)) {
        return "Укажите корректный email.";
    }
    return "";
}

function updateSupportValidationState() {
    const channel = document.getElementById("supportPageContactChannel");
    const value = document.getElementById("supportPageContactValue");
    const subject = document.getElementById("supportPageSubject");
    const message = document.getElementById("supportPageMessage");
    if (!channel || !value || !subject || !message) {
        return "";
    }
    const validationMessage = validateSupportFormData(channel.value, value.value, subject.value, message.value);
    value.setCustomValidity(validationMessage);
    return validationMessage;
}

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

    const config = supportChannelConfig(channel.value);
    value.placeholder = config.placeholder;
    value.inputMode = config.inputMode;
    value.type = config.type;
    value.value = normalizeSupportContactValue(channel.value, value.value);
    updateSupportValidationState();
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
            payload.contactValue = normalizeSupportContactValue(payload.contactChannel, payload.contactValue);
            payload.subject = String(payload.subject || "").trim();
            payload.message = String(payload.message || "").trim();
            const validationMessage = validateSupportFormData(
                payload.contactChannel,
                payload.contactValue,
                payload.subject,
                payload.message
            );
            if (validationMessage) {
                if (status) {
                    status.textContent = validationMessage;
                }
                updateSupportValidationState();
                return;
            }
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
document.getElementById("supportPageContactValue")?.addEventListener("input", () => {
    updateSupportValidationState();
});
document.getElementById("supportPageSubject")?.addEventListener("input", () => {
    updateSupportValidationState();
});
document.getElementById("supportPageMessage")?.addEventListener("input", () => {
    updateSupportValidationState();
});

window.addEventListener("auth-state-changed", (event) => {
    supportPageUser = event.detail ? event.detail.user || null : null;
    updateSupportPageAccess();
});

initSupportPage();
