const VEHICLE_CATEGORY_LABELS = {
    standard: "Стандарт",
    crossover: "Кроссовер",
    premium: "Премиум",
    minivan: "Минивэн 7 мест",
    exclusive: "Эксклюзив",
    electric: "Электро",
    cabriolet: "Кабриолет",
    offroad: "Внедорожник",
    cargo: "Грузовой"
};

function vehicleCategoryLabel(value) {
    return VEHICLE_CATEGORY_LABELS[value] || value || "Без категории";
}

function linesToList(containerId, text, fallback) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }
    const items = String(text || "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
    const finalItems = items.length ? items : [fallback];
    container.innerHTML = finalItems.map((item) => `<li>${escapeHtml(item)}</li>`).join("");
}

function parseLines(text) {
    return String(text || "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
}

function extractSpecValue(lines, regex, fallback) {
    const line = lines.find((item) => regex.test(item));
    if (!line) {
        return fallback;
    }
    const parts = line.split(":");
    if (parts.length > 1) {
        const value = parts.slice(1).join(":").trim();
        return value || fallback;
    }
    return line;
}

function setVehiclePageError(message) {
    const state = document.getElementById("vehiclePageState");
    const content = document.getElementById("vehiclePageContent");
    if (state) {
        state.textContent = message;
        state.classList.remove("hidden");
    }
    if (content) {
        content.classList.add("hidden");
    }
}

function fillVehiclePage(card) {
    document.title = `${card.title} | FredAvto`;
    document.getElementById("vehicleBreadcrumbs").innerHTML =
        `<a href="/services.html">Тарифы</a> / <a href="/services.html">${escapeHtml(vehicleCategoryLabel(card.category))}</a> / <span>${escapeHtml(card.title)}</span>`;
    document.getElementById("vehicleEyebrow").textContent = `Тарифы / ${vehicleCategoryLabel(card.category)}`;
    document.getElementById("vehicleTitle").textContent = card.title;
    document.getElementById("vehicleSubtitle").textContent = card.shortDescription || "Карточка машины FredAvto.";
    document.getElementById("vehiclePrice").textContent = `от ${card.pricePerMinute} BYN / 1 мин.`;
    document.getElementById("vehiclePriceHint").textContent = card.shortDescription || "Базовая ставка для аренды автомобиля.";
    document.getElementById("vehicleDetailDescription").textContent = card.detailDescription || "Описание пока не заполнено.";

    const heroImage = document.getElementById("vehicleHeroImage");
    heroImage.src = normalizeStaticAssetPath(card.imagePath);
    heroImage.alt = card.title;

    const wrapped = card.wrapped === true || card.wrapped === "true";
    document.getElementById("vehicleTags").innerHTML = [
        vehicleCategoryLabel(card.category),
        wrapped ? "С оклейкой" : "Без оклейки",
        card.transmission || "Трансмиссия не указана",
        card.fuelType || "Тип топлива не указан"
    ].map((item) => `<span class="car-tag">${escapeHtml(item)}</span>`).join("");

    linesToList("vehicleMinutePackages", card.minutePackagesText, `1 мин. - ${card.pricePerMinute} BYN`);
    linesToList("vehicleHourPackages", card.hourPackagesText, "Почасовые пакеты пока не указаны.");
    linesToList("vehicleDayPackages", card.dayPackagesText, "Суточные пакеты пока не указаны.");
    linesToList("vehicleConditions", card.conditionsText, "Условия аренды будут добавлены позже.");

    const featureLines = parseLines(card.featuresText);
    const chargingValue = extractSpecValue(featureLines, /заряд/i, "Type-C, Lightning, Micro USB");
    const boosterValue = extractSpecValue(featureLines, /бустер/i, "1");
    const phoneHolderValue = extractSpecValue(featureLines, /держатель|телефон/i, "Есть");

    const visibleFeatures = featureLines.filter((item) => !/(заряд|бустер|держатель|телефон)/i.test(item));
    linesToList("vehicleFeatures", visibleFeatures.join("\n"), "Особенности машины будут добавлены позже.");

    const gearboxNode = document.getElementById("vehicleSpecGearbox");
    const engineNode = document.getElementById("vehicleSpecEngine");
    const chargingNode = document.getElementById("vehicleSpecCharging");
    const boosterNode = document.getElementById("vehicleSpecBooster");
    const phoneHolderNode = document.getElementById("vehicleSpecPhoneHolder");

    if (gearboxNode) gearboxNode.textContent = card.transmission || "Не указано";
    if (engineNode) engineNode.textContent = card.engine || "Не указано";
    if (chargingNode) chargingNode.textContent = chargingValue;
    if (boosterNode) boosterNode.textContent = boosterValue;
    if (phoneHolderNode) phoneHolderNode.textContent = phoneHolderValue;
}

async function initVehicleDetailPage() {
    try {
        const user = await loadSessionUser();
        setupPageHeader(user);
    } catch {
        setupGuestPageHeader();
    }

    const params = new URLSearchParams(window.location.search);
    const slug = params.get("slug");
    if (!slug) {
        setVehiclePageError("Не указан slug карточки машины.");
        return;
    }

    try {
        const card = await pageRequest(`/api/vehicle-cards/public/${encodeURIComponent(slug)}`);
        fillVehiclePage(card);
    } catch (error) {
        setVehiclePageError(extractErrorMessage(error, "Не удалось загрузить карточку машины."));
    }
}

function initVehicleRateTabs() {
    const tablist = document.querySelector(".car-rate-tabs");
    if (!tablist) {
        return;
    }
    const tabs = [...tablist.querySelectorAll(".car-rate-tab")];
    const panels = [...document.querySelectorAll("[data-rate-panel]")];
    if (!tabs.length || !panels.length) {
        return;
    }

    function activate(rate) {
        tabs.forEach((tab) => {
            const on = tab.getAttribute("data-rate") === rate;
            tab.classList.toggle("is-active", on);
            tab.setAttribute("aria-selected", on ? "true" : "false");
        });
        panels.forEach((panel) => {
            const on = panel.getAttribute("data-rate-panel") === rate;
            panel.classList.toggle("is-active", on);
            panel.hidden = !on;
        });
    }

    tabs.forEach((tab) => {
        tab.addEventListener("click", () => activate(tab.getAttribute("data-rate")));
    });
}

initVehicleRateTabs();
initVehicleDetailPage();
