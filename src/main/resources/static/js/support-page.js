let supportPageUser = null;

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
            setButtonBusy(submitButton, true, "Отправка...");
            if (status) {
                status.textContent = "";
            }

            const payload = Object.fromEntries(new FormData(supportPageForm).entries());
            await pageRequest("/api/support-requests", {
                method: "POST",
                body: JSON.stringify(payload)
            });

            supportPageForm.reset();
            if (status) {
                status.textContent = "Обращение отправлено. Мы свяжемся с вами.";
            }
        } catch (error) {
            if (status) {
                status.textContent = extractErrorMessage(error, "Не удалось отправить обращение.");
            }
        } finally {
            setButtonBusy(submitButton, false, "Отправка...");
        }
    });
}

window.addEventListener("auth-state-changed", (event) => {
    supportPageUser = event.detail ? event.detail.user || null : null;
    updateSupportPageAccess();
});

initSupportPage();
