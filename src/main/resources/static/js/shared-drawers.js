(function () {
    const MENU_ITEMS = [
        {label: "ТАРИФЫ", href: "/index.html#pricing"},
        {label: "ЗОНЫ ЗАВЕРШЕНИЯ", href: "/map.html"},
        {label: "ПОДСКАЗКИ", href: "/tips.html"},
        {label: "ПРАВИЛА ЗАПРАВКИ", href: "/fueling.html"},
        {label: "НОВОСТИ", href: "/news.html"},
        {label: "КОНТАКТЫ", href: "/index.html#contacts"},
        {label: "УСЛУГИ", href: "/services.html"}
    ];

    const SUPPORT_CONTACTS = [
        {icon: "/images/site/support-phone.png", title: "Телефон", value: "+375 (29) 823-44-65", href: "tel:+375298234465"},
        {icon: "/images/site/support-telegram.png", title: "Telegram", value: "@riiizus", href: "https://t.me/riiizus"},
        {icon: "/images/site/support-viber.png", title: "Viber", value: "+375 (29) 823-44-65", href: "viber://chat?number=%2B375298234465"},
        {icon: "/images/site/support-whatsapp.png", title: "WhatsApp", value: "+375 (29) 823-44-65", href: "https://wa.me/375298234465"}
    ];

    let activeDrawer = null;
    let currentUser = null;

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    async function drawerRequest(url, options = {}) {
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

    function ensureDrawerMarkup() {
        if (document.getElementById("drawerBackdrop")) {
            return;
        }

        const menuItemsMarkup = MENU_ITEMS.map((item) => `
            <a class="drawer-menu__link" href="${item.href}">${escapeHtml(item.label)}</a>
        `).join("");

        const supportCardsMarkup = SUPPORT_CONTACTS.map((item) => `
            <a class="support-card" href="${item.href}" target="_blank" rel="noreferrer">
                <span class="support-card__icon support-card__icon_image">
                    <img src="${item.icon}" alt="${escapeHtml(item.title)}">
                </span>
                <span class="support-card__content">
                    <span class="support-card__title">${escapeHtml(item.title)}</span>
                    <strong class="support-card__value">${escapeHtml(item.value)}</strong>
                </span>
            </a>
        `).join("");

        const markup = `
            <div id="drawerBackdrop" class="drawer-backdrop hidden"></div>

            <aside id="menuDrawer" class="drawer drawer-menu hidden" aria-hidden="true">
                <div class="drawer__inner">
                    <div class="drawer__header">
                        <a class="drawer-brand" href="/index.html">
                            <img src="/images/site/logo.png" alt="Логотип FredAvto">
                            <span>FredAvto</span>
                        </a>
                        <button class="drawer-close" type="button" data-close-drawer="menu" aria-label="Закрыть меню">x</button>
                    </div>
                    <nav class="drawer-menu__list">
                        ${menuItemsMarkup}
                    </nav>
                    <button class="drawer-app-button" type="button">Скачать приложение</button>
                </div>
            </aside>

            <aside id="supportDrawer" class="drawer drawer-support hidden" aria-hidden="true">
                <div class="drawer__inner">
                    <div class="drawer__header">
                        <h2 class="drawer-title">ТЕХПОДДЕРЖКА</h2>
                        <button class="drawer-close" type="button" data-close-drawer="support" aria-label="Закрыть техподдержку">x</button>
                    </div>
                    <div class="support-stack">
                        ${supportCardsMarkup}
                        <a class="support-card support-card_action" href="/support.html">
                            <span class="support-card__icon support-card__icon_image">
                                <img src="/images/site/support-form.png" alt="Форма обращения">
                            </span>
                            <span class="support-card__content">
                                <span class="support-card__title">Заполнить обращение</span>
                                <strong class="support-card__value">Открыть страницу</strong>
                            </span>
                        </a>
                    </div>
                    <section class="support-form-panel">
                        <p id="supportAuthHint" class="support-form-panel__hint"></p>
                        <button id="supportLoginPrompt" class="btn btn-small btn-secondary hidden" type="button">Войти</button>
                    </section>
                </div>
            </aside>
        `;

        document.body.insertAdjacentHTML("beforeend", markup);
    }

    function ensureHeaderControls() {
        const authBlocks = document.querySelectorAll(".auth-block");
        authBlocks.forEach((authBlock) => {
            if (authBlock.querySelector(".support-drawer-toggle")) {
                return;
            }

            const supportButton = document.createElement("button");
            supportButton.type = "button";
            supportButton.className = "btn btn-small btn-support support-drawer-toggle";
            supportButton.textContent = "Техподдержка";
            supportButton.setAttribute("aria-label", "Открыть техподдержку");

            const drawerMenuButton = document.createElement("button");
            drawerMenuButton.type = "button";
            drawerMenuButton.className = "drawer-menu-toggle";
            drawerMenuButton.setAttribute("aria-label", "Открыть боковое меню");
            drawerMenuButton.innerHTML = "<span></span><span></span><span></span>";

            const logoutButton = authBlock.querySelector("#logoutBtn, #pageLogoutBtn");
            if (logoutButton && logoutButton.nextSibling) {
                authBlock.insertBefore(supportButton, logoutButton.nextSibling);
                authBlock.insertBefore(drawerMenuButton, supportButton.nextSibling);
            } else {
                authBlock.append(supportButton, drawerMenuButton);
            }
        });
    }

    function setDrawerOpen(type, isOpen) {
        const backdrop = document.getElementById("drawerBackdrop");
        const drawer = document.getElementById(type === "menu" ? "menuDrawer" : "supportDrawer");
        if (!backdrop || !drawer) {
            return;
        }

        if (isOpen) {
            activeDrawer = type;
            backdrop.classList.remove("hidden");
            drawer.classList.remove("hidden");
            requestAnimationFrame(() => {
                backdrop.classList.add("is-visible");
                drawer.classList.add("is-open");
            });
            drawer.setAttribute("aria-hidden", "false");
            document.body.classList.add("drawer-open");
            return;
        }

        backdrop.classList.remove("is-visible");
        drawer.classList.remove("is-open");
        drawer.setAttribute("aria-hidden", "true");
        if (activeDrawer === type) {
            activeDrawer = null;
        }
        window.setTimeout(() => {
            if (!activeDrawer) {
                backdrop.classList.add("hidden");
                document.body.classList.remove("drawer-open");
            }
            drawer.classList.add("hidden");
        }, 240);
    }

    function closeActiveDrawer() {
        if (activeDrawer) {
            setDrawerOpen(activeDrawer, false);
        }
    }

    function syncSupportFormState() {
        const authHint = document.getElementById("supportAuthHint");
        const loginPrompt = document.getElementById("supportLoginPrompt");
        if (!authHint || !loginPrompt) {
            return;
        }

        if (currentUser) {
            authHint.textContent = `Вы авторизованы как ${currentUser.username || currentUser.email || "пользователь"}. Форму можно заполнить на отдельной странице.`;
            loginPrompt.classList.add("hidden");
        } else {
            authHint.textContent = "Форма обращения доступна только после регистрации и входа в аккаунт.";
            loginPrompt.classList.remove("hidden");
        }
    }

    function bindDrawerEvents() {
        document.addEventListener("click", (event) => {
            const menuToggle = event.target.closest(".drawer-menu-toggle");
            const supportToggle = event.target.closest(".support-drawer-toggle");
            const closeButton = event.target.closest("[data-close-drawer]");
            const backdrop = event.target.closest("#drawerBackdrop");

            if (menuToggle) {
                setDrawerOpen("menu", true);
                return;
            }
            if (supportToggle) {
                setDrawerOpen("support", true);
                return;
            }
            if (closeButton) {
                setDrawerOpen(closeButton.dataset.closeDrawer, false);
                return;
            }
            if (backdrop) {
                closeActiveDrawer();
            }
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeActiveDrawer();
            }
        });

        const supportLoginPrompt = document.getElementById("supportLoginPrompt");

        if (supportLoginPrompt) {
            supportLoginPrompt.addEventListener("click", () => {
                closeActiveDrawer();
                const loginOpenButton = document.getElementById("openLogin");
                if (loginOpenButton) {
                    loginOpenButton.click();
                }
            });
        }

        document.querySelectorAll(".drawer-menu__link").forEach((link) => {
            link.addEventListener("click", () => closeActiveDrawer());
        });
    }

    async function loadDrawerUser() {
        try {
            currentUser = await drawerRequest("/api/auth/me");
        } catch {
            currentUser = null;
        }
        syncSupportFormState();
    }

    function initFromAuthEvent() {
        window.addEventListener("auth-state-changed", (event) => {
            currentUser = event.detail ? event.detail.user || null : null;
            syncSupportFormState();
        });
    }

    function initSharedDrawers() {
        ensureDrawerMarkup();
        ensureHeaderControls();
        bindDrawerEvents();
        initFromAuthEvent();
        loadDrawerUser();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initSharedDrawers);
    } else {
        initSharedDrawers();
    }
})();
