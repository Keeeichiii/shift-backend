document.querySelectorAll(".accordion-card").forEach((card, index) => {
    const toggle = card.querySelector(".accordion-toggle");
    const content = card.querySelector(".accordion-content");
    if (!toggle || !content) {
        return;
    }

    const expandedInitially = index === 0;
    card.classList.toggle("is-open", expandedInitially);
    toggle.setAttribute("aria-expanded", expandedInitially ? "true" : "false");
    content.classList.toggle("hidden", !expandedInitially);

    toggle.addEventListener("click", () => {
        const nextState = toggle.getAttribute("aria-expanded") !== "true";
        toggle.setAttribute("aria-expanded", nextState ? "true" : "false");
        content.classList.toggle("hidden", !nextState);
        card.classList.toggle("is-open", nextState);
    });
});

(async () => {
    try {
        const sessionUser = await loadSessionUser();
        setupPageHeader(sessionUser);
    } catch {
        setupGuestPageHeader();
    }
})();

