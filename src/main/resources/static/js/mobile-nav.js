
(function () {
    function qs(id) {
        return document.getElementById(id);
    }

    function init() {
        const toggle = qs("menuToggle");
        const menuLeft = qs("menuLeft");
        const menuRight = qs("menuRight");
        const header = document.querySelector(".site-header");
        if (!toggle || !menuLeft || !menuRight || !header) {
            return;
        }
        if (toggle.dataset.navBound === "true") {
            return;
        }
        toggle.dataset.navBound = "true";

        let backdrop = qs("navBackdrop");
        if (!backdrop) {
            backdrop = document.createElement("div");
            backdrop.id = "navBackdrop";
            backdrop.className = "nav-backdrop";
            backdrop.setAttribute("aria-hidden", "true");
            document.body.appendChild(backdrop);
        }

        function isMobileNav() {
            return window.matchMedia("(max-width: 780px)").matches;
        }

        function setOpen(open) {
            menuLeft.classList.toggle("open", open);
            menuRight.classList.toggle("open", open);
            toggle.setAttribute("aria-expanded", open ? "true" : "false");
            document.body.classList.toggle("nav-open", open && isMobileNav());
            backdrop.classList.toggle("is-visible", open && isMobileNav());
            backdrop.setAttribute("aria-hidden", open ? "false" : "true");
        }

        function close() {
            setOpen(false);
        }

        toggle.addEventListener("click", (e) => {
            e.stopPropagation();
            const next = !(menuLeft.classList.contains("open") && menuRight.classList.contains("open"));
            setOpen(next);
        });

        backdrop.addEventListener("click", close);

        document.addEventListener("click", (e) => {
            if (!isMobileNav() || !menuLeft.classList.contains("open")) {
                return;
            }
            if (header.contains(e.target) || backdrop.contains(e.target)) {
                return;
            }
            close();
        });

        [menuLeft, menuRight].forEach((nav) => {
            nav.querySelectorAll("a").forEach((a) => {
                a.addEventListener("click", () => {
                    if (isMobileNav()) {
                        close();
                    }
                });
            });
        });

        window.addEventListener("resize", () => {
            if (!isMobileNav()) {
                close();
            }
        });

        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && menuLeft.classList.contains("open")) {
                close();
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
