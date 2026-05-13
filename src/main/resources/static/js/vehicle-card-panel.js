const PANEL_VEHICLE_CATEGORY_LABELS = {
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

/** Имена подпапок в static/images/cars/… (как на сервере при сохранении). */
const PANEL_VEHICLE_CATEGORY_FOLDERS = {
    standard: "стандарт",
    crossover: "кроссовер",
    premium: "премиум",
    minivan: "минивэн 7 мест",
    exclusive: "эксклюзив",
    electric: "электро",
    cabriolet: "кабриолет",
    offroad: "внедорожник",
    cargo: "грузовой",
    long_booking: "долгое бронирование"
};

function panelVehicleCategoryLabel(value) {
    return PANEL_VEHICLE_CATEGORY_LABELS[value] || value || "Без категории";
}

function panelVehicleCategoryFolderName(value) {
    return PANEL_VEHICLE_CATEGORY_FOLDERS[value] || value || "";
}

function updateVehicleCardImageFolderHint(prefix) {
    const el = document.getElementById(`${prefix}CardImageFolderHint`);
    if (!el) {
        return;
    }
    const category = document.getElementById(`${prefix}CardCategory`)?.value || "standard";
    const folder = panelVehicleCategoryFolderName(category);
    el.textContent = `После выбора файла он сохранится в src/main/resources/static/images/cars/${folder}/ (URL /images/cars/${folder}/…). Диалог проводника на ПК в папку проекта не переходит.`;
}

function parseMultiline(value) {
    return String(value || "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
}

function isInlineSpecLine(line) {
    return /^(заряд(ка)?|бустер|держатель(\s+для\s+телефона)?|телефон)\s*:/i.test(String(line || "").trim());
}

function takeInlineSpecValue(lines, regex) {
    const index = lines.findIndex((line) => regex.test(line));
    if (index < 0) {
        return "";
    }
    const [line] = lines.splice(index, 1);
    const parts = line.split(":");
    return parts.length > 1 ? parts.slice(1).join(":").trim() : "";
}

function initPanelTabs(tabsRoot) {
    if (!tabsRoot || tabsRoot.dataset.bound === "true") {
        return;
    }

    const buttons = Array.from(tabsRoot.querySelectorAll(".panel-tab"));
    buttons.forEach((button) => {
        button.addEventListener("click", () => {
            const targetId = button.dataset.panelTarget;
            if (!targetId) {
                return;
            }

            buttons.forEach((item) => item.classList.toggle("is-active", item === button));
            document.querySelectorAll(".panel-section").forEach((section) => {
                section.classList.toggle("hidden", section.id !== targetId);
            });
        });
    });

    tabsRoot.dataset.bound = "true";
}

function renderVehicleCardList(containerId, cards, handlers = {}) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }

    if (!cards.length) {
        container.innerHTML = `<div class="vehicle-card-item"><strong>Карточек пока нет</strong><p>Создайте первую карточку машины через форму слева.</p></div>`;
        return;
    }

    container.innerHTML = cards.map((card) => `
        <article class="vehicle-card-item">
            <img class="vehicle-card-item__image" src="${escapeHtml(normalizeStaticAssetPath(card.imagePath))}" alt="${escapeHtml(card.title)}">
            <div class="vehicle-card-item__body">
                <strong>${escapeHtml(card.title)}</strong>
                <p>${escapeHtml(panelVehicleCategoryLabel(card.category))} · ${card.wrapped ? "С оклейкой" : "Без оклейки"} · ${escapeHtml(card.pricePerMinute)} BYN/мин</p>
                <div class="meta-badges">
                    <span class="badge badge_role">${escapeHtml(card.slug)}</span>
                    <span class="badge badge_role">${card.published ? "Опубликовано" : "Черновик"}</span>
                    ${card.badge ? `<span class="badge badge_role">${escapeHtml(card.badge)}</span>` : ""}
                </div>
                <div class="panel-actions" data-card-id="${escapeHtml(card.id)}">
                    <a class="btn btn-small" href="/vehicle.html?slug=${encodeURIComponent(card.slug)}">Открыть</a>
                    <button class="btn btn-small btn-ghost vehicle-card-edit" type="button">Редактировать</button>
                    <button class="btn btn-small btn-danger vehicle-card-delete" type="button">Удалить</button>
                </div>
            </div>
        </article>
    `).join("");

    container.querySelectorAll(".vehicle-card-edit").forEach((button) => {
        button.addEventListener("click", () => {
            const cardId = Number(button.closest("[data-card-id]").dataset.cardId);
            const card = cards.find((item) => item.id === cardId);
            if (card && handlers.onEdit) {
                handlers.onEdit(card);
            }
        });
    });

    container.querySelectorAll(".vehicle-card-delete").forEach((button) => {
        button.addEventListener("click", () => {
            const cardId = Number(button.closest("[data-card-id]").dataset.cardId);
            const card = cards.find((item) => item.id === cardId);
            if (card && handlers.onDelete) {
                handlers.onDelete(card);
            }
        });
    });
}

function buildVehicleCardPayload(form) {
    const formData = new FormData(form);
    const chargingValue = String(formData.get("charging") || "").trim();
    const boosterValue = String(formData.get("booster") || "").trim();
    const phoneHolderValue = String(formData.get("phoneHolder") || "").trim();
    const baseFeatures = parseMultiline(formData.get("featuresText"))
        .filter((line) => !isInlineSpecLine(line));
    if (chargingValue) {
        baseFeatures.push(`Зарядка: ${chargingValue}`);
    }
    if (boosterValue) {
        baseFeatures.push(`Бустер: ${boosterValue}`);
    }
    if (phoneHolderValue) {
        baseFeatures.push(`Держатель для телефона: ${phoneHolderValue}`);
    }

    return {
        title: formData.get("title") || "",
        slug: formData.get("slug") || "",
        category: formData.get("category") || "standard",
        wrapped: formData.get("wrapped") === "true",
        imagePath: formData.get("imagePath") || "",
        pricePerMinute: Number(formData.get("pricePerMinute")),
        badge: formData.get("badge") || null,
        shortDescription: formData.get("shortDescription") || null,
        detailDescription: formData.get("detailDescription") || null,
        transmission: formData.get("transmission") || null,
        fuelType: formData.get("fuelType") || null,
        engine: formData.get("engine") || null,
        conditionsText: formData.get("conditionsText") || null,
        featuresText: baseFeatures.length ? baseFeatures.join("\n") : null,
        minutePackagesText: formData.get("minutePackagesText") || null,
        hourPackagesText: formData.get("hourPackagesText") || null,
        dayPackagesText: formData.get("dayPackagesText") || null,
        published: true
    };
}

function setVehicleCardFormMode(prefix, mode, card = null) {
    const formTitle = document.getElementById(`${prefix}VehicleCardFormTitle`);
    const submitButton = document.getElementById(`${prefix}VehicleCardSubmit`);
    const cancelButton = document.getElementById(`${prefix}VehicleCardCancel`);
    const hiddenId = document.getElementById(`${prefix}VehicleCardId`);

    if (hiddenId) {
        hiddenId.value = mode === "edit" && card ? String(card.id) : "";
    }
    if (formTitle) {
        formTitle.textContent = mode === "edit" ? "Редактировать карточку машины" : "Создать карточку машины";
    }
    if (submitButton) {
        submitButton.textContent = mode === "edit" ? "Сохранить изменения" : "Создать карточку";
    }
    if (cancelButton) {
        cancelButton.classList.toggle("hidden", mode !== "edit");
    }
}

function updateVehicleCardImagePreview(prefix) {
    const hiddenPath = document.getElementById(`${prefix}CardImagePath`);
    const preview = document.getElementById(`${prefix}CardImagePreview`);
    if (!hiddenPath || !preview) {
        return;
    }
    const value = hiddenPath.value.trim();
    if (value) {
        preview.src = normalizeStaticAssetPath(value);
        preview.classList.remove("hidden");
    } else {
        preview.removeAttribute("src");
        preview.classList.add("hidden");
    }
}

function bindVehicleCardImageUpload(prefix) {
    const fileInput = document.getElementById(`${prefix}CardImageFile`);
    const button = document.getElementById(`${prefix}CardImageUploadBtn`);
    const hiddenPath = document.getElementById(`${prefix}CardImagePath`);
    const hint = document.getElementById(`${prefix}CardImageUploadHint`);
    const status = document.getElementById(`${prefix}VehicleCardStatus`);

    if (!fileInput || !button || !hiddenPath || button.dataset.bound === "true") {
        return;
    }
    button.dataset.bound = "true";

    const categorySelect = document.getElementById(`${prefix}CardCategory`);
    if (categorySelect && categorySelect.dataset.folderHintBound !== "true") {
        categorySelect.addEventListener("change", () => updateVehicleCardImageFolderHint(prefix));
        categorySelect.dataset.folderHintBound = "true";
    }

    button.addEventListener("click", () => {
        updateVehicleCardImageFolderHint(prefix);
        fileInput.click();
    });

    fileInput.addEventListener("change", async () => {
        const file = fileInput.files && fileInput.files[0];
        if (!file) {
            return;
        }
        if (hint) {
            hint.textContent = "Загрузка…";
        }
        try {
            const categorySelect = document.getElementById(`${prefix}CardCategory`);
            const category = categorySelect ? categorySelect.value : "";
            const data = await uploadVehicleCardImageFile(file, category);
            const path = data && data.imagePath ? String(data.imagePath) : "";
            if (!path) {
                throw new Error("Сервер не вернул путь к файлу.");
            }
            hiddenPath.value = path;
            updateVehicleCardImagePreview(prefix);
            if (hint) {
                hint.textContent = "Фото загружено.";
            }
        } catch (error) {
            if (hint) {
                hint.textContent = extractErrorMessage(error, "Не удалось загрузить фото.");
            }
            if (status) {
                status.textContent = extractErrorMessage(error, "Не удалось загрузить фото.");
            }
        } finally {
            fileInput.value = "";
        }
    });
}

function fillVehicleCardForm(prefix, card) {
    document.getElementById(`${prefix}CardTitle`).value = card.title || "";
    document.getElementById(`${prefix}CardSlug`).value = card.slug || "";
    document.getElementById(`${prefix}CardCategory`).value = card.category || "standard";
    updateVehicleCardImageFolderHint(prefix);
    document.getElementById(`${prefix}CardWrapped`).value = String(Boolean(card.wrapped));
    document.getElementById(`${prefix}CardImagePath`).value = card.imagePath || "";
    document.getElementById(`${prefix}CardPrice`).value = card.pricePerMinute || "";
    document.getElementById(`${prefix}CardBadge`).value = card.badge || "";
    document.getElementById(`${prefix}CardTransmission`).value = card.transmission || "";
    document.getElementById(`${prefix}CardFuelType`).value = card.fuelType || "";
    document.getElementById(`${prefix}CardEngine`).value = card.engine || "";
    document.getElementById(`${prefix}CardShortDescription`).value = card.shortDescription || "";
    document.getElementById(`${prefix}CardDetailDescription`).value = card.detailDescription || "";
    document.getElementById(`${prefix}CardConditions`).value = card.conditionsText || "";
    const featureLines = parseMultiline(card.featuresText);
    const chargingValue = takeInlineSpecValue(featureLines, /^заряд(ка)?\s*:/i);
    const boosterValue = takeInlineSpecValue(featureLines, /^бустер\s*:/i);
    const phoneHolderValue = takeInlineSpecValue(featureLines, /^держатель(\s+для\s+телефона)?\s*:/i);
    document.getElementById(`${prefix}CardFeatures`).value = featureLines.join("\n");
    const chargingInput = document.getElementById(`${prefix}CardCharging`);
    const boosterInput = document.getElementById(`${prefix}CardBooster`);
    const phoneHolderInput = document.getElementById(`${prefix}CardPhoneHolder`);
    if (chargingInput) chargingInput.value = chargingValue;
    if (boosterInput) boosterInput.value = boosterValue;
    if (phoneHolderInput) phoneHolderInput.value = phoneHolderValue;
    document.getElementById(`${prefix}CardMinutePackages`).value = card.minutePackagesText || "";
    document.getElementById(`${prefix}CardHourPackages`).value = card.hourPackagesText || "";
    document.getElementById(`${prefix}CardDayPackages`).value = card.dayPackagesText || "";
    updateVehicleCardImagePreview(prefix);
}

function resetVehicleCardForm(prefix, options = {}) {
    const form = document.getElementById(`${prefix}VehicleCardForm`);
    const status = document.getElementById(`${prefix}VehicleCardStatus`);
    if (form) {
        form.reset();
    }
    const category = document.getElementById(`${prefix}CardCategory`);
    const wrapped = document.getElementById(`${prefix}CardWrapped`);
    if (category) {
        category.value = "standard";
    }
    updateVehicleCardImageFolderHint(prefix);
    if (wrapped) {
        wrapped.value = "true";
    }
    if (status && !options.preserveStatus) {
        status.textContent = "";
    }
    const hint = document.getElementById(`${prefix}CardImageUploadHint`);
    if (hint) {
        hint.textContent = "";
    }
    updateVehicleCardImagePreview(prefix);
    setVehicleCardFormMode(prefix, "create");
}

function initVehicleCardManager({prefix, listId, refresh}) {
    bindVehicleCardImageUpload(prefix);
    const form = document.getElementById(`${prefix}VehicleCardForm`);
    const status = document.getElementById(`${prefix}VehicleCardStatus`);
    const cancelButton = document.getElementById(`${prefix}VehicleCardCancel`);
    let cards = [];

    async function handleDelete(card) {
        if (!window.confirm(`Удалить карточку "${card.title}"?`)) {
            return;
        }
        try {
            await pageRequest(`/api/vehicle-cards/${card.id}`, {method: "DELETE"});
            if (status) {
                status.textContent = "Карточка машины удалена.";
            }
            if (document.getElementById(`${prefix}VehicleCardId`)?.value === String(card.id)) {
                resetVehicleCardForm(prefix);
            }
            await refresh();
        } catch (error) {
            if (status) {
                status.textContent = extractErrorMessage(error, "Не удалось удалить карточку машины.");
            }
        }
    }

    function setCards(nextCards) {
        cards = nextCards;
        renderVehicleCardList(listId, cards, {
            onEdit(card) {
                fillVehicleCardForm(prefix, card);
                setVehicleCardFormMode(prefix, "edit", card);
                form?.scrollIntoView({behavior: "smooth", block: "start"});
            },
            onDelete: handleDelete
        });
    }

    if (cancelButton && cancelButton.dataset.bound !== "true") {
        cancelButton.addEventListener("click", () => resetVehicleCardForm(prefix));
        cancelButton.dataset.bound = "true";
    }

    if (form && form.dataset.bound !== "true") {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const submitButton = form.querySelector('button[type="submit"]');
            const cardId = document.getElementById(`${prefix}VehicleCardId`)?.value;
            const payload = buildVehicleCardPayload(form);
            const editing = Boolean(cardId);

            try {
                setButtonBusy(submitButton, true, editing ? "Сохранение..." : "Создание...");
                if (status) {
                    status.textContent = "";
                }
                await pageRequest(editing ? `/api/vehicle-cards/${cardId}` : "/api/vehicle-cards", {
                    method: editing ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                if (status) {
                    status.textContent = editing ? "Карточка машины обновлена." : "Карточка машины создана.";
                }
                resetVehicleCardForm(prefix, {preserveStatus: true});
                await refresh();
            } catch (error) {
                if (status) {
                    status.textContent = extractErrorMessage(error, editing
                        ? "Не удалось обновить карточку машины."
                        : "Не удалось создать карточку машины.");
                }
            } finally {
                setButtonBusy(submitButton, false, editing ? "Сохранение..." : "Создание...");
            }
        });
        form.dataset.bound = "true";
    }

    resetVehicleCardForm(prefix);
    updateVehicleCardImageFolderHint(prefix);

    return {
        setCards
    };
}
