(function () {
    const DRAWER_MAIN_LINKS = [
        {label: "Главная", href: "/index.html#hero"},
        {label: "Карта зон", href: "/map.html"},
        {label: "Личный кабинет", href: "/account.html"},
        {label: "Услуги и тарифы", href: "/services.html"},
        {label: "Контакты", href: "/index.html#contacts"}
    ];

    const MENU_ITEMS = [
        {label: "ТАРИФЫ", href: "/index.html#pricing"},
        {label: "ЗОНЫ ЗАВЕРШЕНИЯ", href: "/map.html"},
        {label: "ПОДСКАЗКИ", href: "/tips.html"},
        {label: "ПРАВИЛА ЗАПРАВКИ", href: "/fueling.html"},
        {label: "НОВОСТИ", href: "/news.html"}
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

        const primaryMarkup = DRAWER_MAIN_LINKS.map((item) => `
            <a class="drawer-menu__link drawer-menu__link_primary" href="${item.href}">${escapeHtml(item.label)}</a>
        `).join("");

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
                        <button class="drawer-close" type="button" data-close-drawer="menu" aria-label="Закрыть меню">×</button>
                    </div>
                    <nav class="drawer-menu__list drawer-menu__list_primary" aria-label="Основное меню">
                        ${primaryMarkup}
                        <a id="drawerPanelNav" class="drawer-menu__link drawer-menu__link_primary hidden" href="#">Панель</a>
                    </nav>
                    <p class="drawer-nav-section__title">Ещё на сайте</p>
                    <nav class="drawer-menu__list drawer-menu__list_secondary" aria-label="Дополнительно">
                        ${menuItemsMarkup}
                    </nav>
                    <div class="drawer-auth-panel">
                        <div id="drawerAuthGuest" class="drawer-auth-guest">
                            <button type="button" class="btn drawer-mirror-login">Войти</button>
                            <button type="button" class="btn btn-secondary drawer-mirror-register">Регистрация</button>
                        </div>
                        <div id="drawerAuthUser" class="drawer-auth-user hidden">
                            <p id="drawerUserLabel" class="drawer-auth-user-label"></p>
                            <button type="button" class="btn btn-small drawer-mirror-logout">Выйти</button>
                        </div>
                        <button type="button" class="btn btn-small btn-support drawer-open-support-in-menu" data-open-support-from-menu>
                            Техподдержка
                        </button>
                    </div>
                    <button class="drawer-app-button" type="button">Скачать приложение</button>
                </div>
            </aside>

            <aside id="supportDrawer" class="drawer drawer-support hidden" aria-hidden="true">
                <div class="drawer__inner">
                    <div class="drawer__header">
                        <h2 class="drawer-title">ТЕХПОДДЕРЖКА</h2>
                        <button class="drawer-close" type="button" data-close-drawer="support" aria-label="Закрыть техподдержку">×</button>
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

    function syncDrawerPanelLink() {
        const src = document.getElementById("panelLink");
        const dst = document.getElementById("drawerPanelNav");
        if (!dst) {
            return;
        }
        if (!src || src.classList.contains("hidden")) {
            dst.classList.add("hidden");
            dst.setAttribute("href", "#");
            dst.textContent = "Панель";
        } else {
            dst.classList.remove("hidden");
            dst.href = src.getAttribute("href") || "#";
            dst.textContent = src.textContent || "Панель";
        }
    }

    function headerShowsLoggedIn() {
        const lo = document.getElementById("logoutBtn");
        if (lo && !lo.classList.contains("hidden")) {
            return true;
        }
        const pl = document.getElementById("pageLogoutBtn");
        return !!(pl && !pl.classList.contains("hidden"));
    }

    function syncDrawerAuthVisibility() {
        const guest = document.getElementById("drawerAuthGuest");
        const userBox = document.getElementById("drawerAuthUser");
        if (!guest || !userBox) {
            return;
        }
        if (headerShowsLoggedIn()) {
            guest.classList.add("hidden");
            userBox.classList.remove("hidden");
            const dl = document.getElementById("drawerUserLabel");
            if (dl) {
                const ul = document.getElementById("currentUserLabel") || document.getElementById("pageUserLabel");
                if (ul) {
                    dl.textContent = ul.textContent || "Пользователь";
                } else if (currentUser) {
                    dl.textContent = currentUser.username || currentUser.email || "Пользователь";
                } else {
                    dl.textContent = "Пользователь";
                }
            }
        } else {
            guest.classList.remove("hidden");
            userBox.classList.add("hidden");
        }
    }

    function bindDrawerAuthActions() {
        document.querySelector(".drawer-mirror-login")?.addEventListener("click", () => {
            closeActiveDrawer();
            window.setTimeout(() => {
                const t = document.getElementById("openLogin");
                if (t) {
                    t.click();
                } else {
                    window.location.href = "/index.html";
                }
            }, 240);
        });

        document.querySelector(".drawer-mirror-register")?.addEventListener("click", () => {
            closeActiveDrawer();
            window.setTimeout(() => {
                const t = document.getElementById("openRegister");
                if (t) {
                    t.click();
                } else {
                    window.location.href = "/index.html?register=1";
                }
            }, 240);
        });

        document.querySelector(".drawer-mirror-logout")?.addEventListener("click", () => {
            const t = document.getElementById("logoutBtn") || document.getElementById("pageLogoutBtn");
            if (t) {
                closeActiveDrawer();
                window.setTimeout(() => t.click(), 200);
            }
        });
    }

    function ensureMobileHeaderBurger() {
        document.querySelectorAll(".header-row").forEach((row) => {
            if (row.querySelector(".header-drawer-burger")) {
                return;
            }
            const auth = row.querySelector(".auth-block");
            if (!auth) {
                return;
            }
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "drawer-menu-toggle header-drawer-burger";
            btn.setAttribute("aria-label", "Открыть меню");
            btn.innerHTML = "<span></span><span></span><span></span>";
            auth.insertAdjacentElement("afterend", btn);
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
            syncDrawerAuthVisibility();
            syncDrawerPanelLink();
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
            if (event.target.closest("[data-open-support-from-menu]")) {
                event.preventDefault();
                setDrawerOpen("menu", false);
                window.setTimeout(() => setDrawerOpen("support", true), 220);
                return;
            }

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

        const menuDrawer = document.getElementById("menuDrawer");
        if (menuDrawer) {
            menuDrawer.addEventListener("click", (e) => {
                if (e.target.closest(".drawer-menu__link")) {
                    closeActiveDrawer();
                }
            });
        }
    }

    async function loadDrawerUser() {
        try {
            currentUser = await drawerRequest("/api/auth/me");
        } catch {
            currentUser = null;
        }
        syncSupportFormState();
        syncDrawerAuthVisibility();
    }

    function initFromAuthEvent() {
        window.addEventListener("auth-state-changed", (event) => {
            currentUser = event.detail ? event.detail.user || null : null;
            syncSupportFormState();
            syncDrawerPanelLink();
            syncDrawerAuthVisibility();
        });
    }

    function initSharedDrawers() {
        ensureDrawerMarkup();
        ensureMobileHeaderBurger();
        bindDrawerAuthActions();
        bindDrawerEvents();
        initFromAuthEvent();
        syncDrawerPanelLink();
        syncDrawerAuthVisibility();
        loadDrawerUser();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initSharedDrawers);
    } else {
        initSharedDrawers();
    }
})();
