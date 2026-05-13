const CATEGORY_LABELS = {
    all: "Все типы",
    standard: "Стандарт",
    crossover: "Кроссовер",
    premium: "Премиум",
    minivan: "Минивэн 7 мест",
    exclusive: "Эксклюзив",
    electric: "Электро",
    cabriolet: "Кабриолет",
    offroad: "Внедорожник",
    cargo: "Грузовой",
    long_booking: "Долгое бронирование"
};

function categoryLabel(value) {
    return CATEGORY_LABELS[value] || value || "Без категории";
}

function renderCatalogCards(cards) {
    const grid = document.getElementById("catalogGrid");
    if (!grid) {
        return [];
    }

    grid.innerHTML = cards.map((card) => {
        const wrapped = card.wrapped === true || card.wrapped === "true";
        const wrapKey = wrapped ? "wrapped" : "plain";
        const wrapLabel = wrapped ? "с оклейкой" : "без оклейки";
        return `
        <a class="catalog-card" href="/vehicle.html?slug=${encodeURIComponent(card.slug)}" data-type="${escapeHtml(card.category)}" data-wrap="${wrapKey}">
            ${card.badge ? `<span class="catalog-card__badge">${escapeHtml(card.badge)}</span>` : ""}
            <div class="catalog-card__image" style="background-image: url('${escapeHtml(normalizeStaticAssetPath(card.imagePath))}');"></div>
            <h2 class="catalog-card__name">${escapeHtml(card.title)}</h2>
            <p class="catalog-card__meta">${escapeHtml(categoryLabel(card.category))}, ${wrapLabel}</p>
            <p class="catalog-card__price">от ${escapeHtml(card.pricePerMinute)} BYN <span>/ 1 мин.</span></p>
        </a>
    `;
    }).join("");

    return Array.from(grid.querySelectorAll(".catalog-card"));
}

function setupCatalogFilters(cards) {
    const buttons = Array.from(document.querySelectorAll("[data-filter-group][data-filter-value]"));
    const emptyState = document.getElementById("catalogEmpty");

    if (!buttons.length) {
        return;
    }

    const filters = {
        type: "all",
        wrap: "all"
    };

    function renderCards() {
        let visibleCount = 0;

        cards.forEach((card) => {
            const typeMatches = filters.type === "all" || card.dataset.type === filters.type;
            const wrapMatches = filters.wrap === "all" || card.dataset.wrap === filters.wrap;
            const visible = typeMatches && wrapMatches;

            card.classList.toggle("hidden", !visible);
            if (visible) {
                visibleCount += 1;
            }
        });

        if (emptyState) {
            emptyState.classList.toggle("hidden", visibleCount > 0);
        }
    }

    buttons.forEach((button) => {
        button.addEventListener("click", () => {
            const group = button.dataset.filterGroup;
            const value = button.dataset.filterValue;
            if (!group || !value) {
                return;
            }

            filters[group] = value;

            buttons
                .filter((item) => item.dataset.filterGroup === group)
                .forEach((item) => item.classList.toggle("is-active", item === button));

            renderCards();
        });
    });

    renderCards();
}

async function initServicesPage() {
    try {
        const user = await loadSessionUser();
        setupPageHeader(user);
    } catch {
        setupGuestPageHeader();
    }

    try {
        const cards = await pageRequest("/api/vehicle-cards/public");
        const cardNodes = renderCatalogCards(cards);
        setupCatalogFilters(cardNodes);
    } catch (error) {
        const grid = document.getElementById("catalogGrid");
        const emptyState = document.getElementById("catalogEmpty");
        if (grid) {
            grid.innerHTML = "";
        }
        if (emptyState) {
            emptyState.textContent = extractErrorMessage(error, "Не удалось загрузить карточки машин.");
            emptyState.classList.remove("hidden");
        }
    }
}

initServicesPage();
