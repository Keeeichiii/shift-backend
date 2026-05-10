function renderNewsPanelList(containerId, newsItems, handlers = {}) {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }

    if (!newsItems.length) {
        container.innerHTML = `<div class="news-panel-item"><strong>Новостей пока нет</strong><p>Создайте первую новость через форму слева.</p></div>`;
        return;
    }

    container.innerHTML = newsItems.map((item) => `
        <article class="news-panel-item">
            <div class="news-panel-item__body">
                <strong>${escapeHtml(item.title)}</strong>
                <p>${escapeHtml(item.summary)}</p>
                <div class="meta-badges">
                    <span class="badge badge_role">${escapeHtml(item.slug)}</span>
                    <span class="badge badge_role">${item.published ? "Опубликовано" : "Черновик"}</span>
                    <span class="badge badge_role">${escapeHtml(formatDateTime(item.updatedAt || item.createdAt))}</span>
                </div>
                <div class="panel-actions" data-news-id="${escapeHtml(item.id)}">
                    <a class="btn btn-small" href="/news.html">Открыть</a>
                    <button class="btn btn-small btn-ghost news-edit" type="button">Редактировать</button>
                    <button class="btn btn-small btn-danger news-delete" type="button">Удалить</button>
                </div>
            </div>
        </article>
    `).join("");

    container.querySelectorAll(".news-edit").forEach((button) => {
        button.addEventListener("click", () => {
            const newsId = Number(button.closest("[data-news-id]").dataset.newsId);
            const item = newsItems.find((entry) => entry.id === newsId);
            if (item && handlers.onEdit) {
                handlers.onEdit(item);
            }
        });
    });

    container.querySelectorAll(".news-delete").forEach((button) => {
        button.addEventListener("click", () => {
            const newsId = Number(button.closest("[data-news-id]").dataset.newsId);
            const item = newsItems.find((entry) => entry.id === newsId);
            if (item && handlers.onDelete) {
                handlers.onDelete(item);
            }
        });
    });
}

function buildNewsPayload(form) {
    const formData = new FormData(form);
    return {
        title: formData.get("title") || "",
        slug: formData.get("slug") || "",
        summary: formData.get("summary") || "",
        content: formData.get("content") || "",
        published: formData.get("published") === "true"
    };
}

function setNewsFormMode(prefix, mode, item = null) {
    const formTitle = document.getElementById(`${prefix}NewsFormTitle`);
    const submitButton = document.getElementById(`${prefix}NewsSubmit`);
    const cancelButton = document.getElementById(`${prefix}NewsCancel`);
    const hiddenId = document.getElementById(`${prefix}NewsId`);

    if (hiddenId) {
        hiddenId.value = mode === "edit" && item ? String(item.id) : "";
    }
    if (formTitle) {
        formTitle.textContent = mode === "edit" ? "Редактировать новость" : "Создать новость";
    }
    if (submitButton) {
        submitButton.textContent = mode === "edit" ? "Сохранить новость" : "Создать новость";
    }
    if (cancelButton) {
        cancelButton.classList.toggle("hidden", mode !== "edit");
    }
}

function fillNewsForm(prefix, item) {
    document.getElementById(`${prefix}NewsTitle`).value = item.title || "";
    document.getElementById(`${prefix}NewsSlug`).value = item.slug || "";
    document.getElementById(`${prefix}NewsSummary`).value = item.summary || "";
    document.getElementById(`${prefix}NewsContent`).value = item.content || "";
    document.getElementById(`${prefix}NewsPublished`).value = String(Boolean(item.published));
}

function resetNewsForm(prefix, options = {}) {
    const form = document.getElementById(`${prefix}NewsForm`);
    const status = document.getElementById(`${prefix}NewsStatus`);
    if (form) {
        form.reset();
    }
    const published = document.getElementById(`${prefix}NewsPublished`);
    if (published) {
        published.value = "true";
    }
    if (status && !options.preserveStatus) {
        status.textContent = "";
    }
    setNewsFormMode(prefix, "create");
}

function initNewsManager({prefix, listId, refresh}) {
    const form = document.getElementById(`${prefix}NewsForm`);
    const status = document.getElementById(`${prefix}NewsStatus`);
    const cancelButton = document.getElementById(`${prefix}NewsCancel`);
    let newsItems = [];

    async function handleDelete(item) {
        if (!window.confirm(`Удалить новость "${item.title}"?`)) {
            return;
        }
        try {
            await pageRequest(`/api/news/${item.id}`, {method: "DELETE"});
            if (status) {
                status.textContent = "Новость удалена.";
            }
            if (document.getElementById(`${prefix}NewsId`)?.value === String(item.id)) {
                resetNewsForm(prefix);
            }
            await refresh();
        } catch (error) {
            if (status) {
                status.textContent = extractErrorMessage(error, "Не удалось удалить новость.");
            }
        }
    }

    function setNews(nextNewsItems) {
        newsItems = nextNewsItems;
        renderNewsPanelList(listId, newsItems, {
            onEdit(item) {
                fillNewsForm(prefix, item);
                setNewsFormMode(prefix, "edit", item);
                form?.scrollIntoView({behavior: "smooth", block: "start"});
            },
            onDelete: handleDelete
        });
    }

    if (cancelButton && cancelButton.dataset.bound !== "true") {
        cancelButton.addEventListener("click", () => resetNewsForm(prefix));
        cancelButton.dataset.bound = "true";
    }

    if (form && form.dataset.bound !== "true") {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const submitButton = form.querySelector('button[type="submit"]');
            const newsId = document.getElementById(`${prefix}NewsId`)?.value;
            const payload = buildNewsPayload(form);
            const editing = Boolean(newsId);

            try {
                setButtonBusy(submitButton, true, editing ? "Сохранение..." : "Создание...");
                if (status) {
                    status.textContent = "";
                }
                await pageRequest(editing ? `/api/news/${newsId}` : "/api/news", {
                    method: editing ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                });
                if (status) {
                    status.textContent = editing ? "Новость обновлена." : "Новость создана.";
                }
                resetNewsForm(prefix, {preserveStatus: true});
                await refresh();
            } catch (error) {
                if (status) {
                    status.textContent = extractErrorMessage(error, editing
                        ? "Не удалось обновить новость."
                        : "Не удалось создать новость.");
                }
            } finally {
                setButtonBusy(submitButton, false, editing ? "Сохранение..." : "Создание...");
            }
        });
        form.dataset.bound = "true";
    }

    resetNewsForm(prefix);

    return {
        setNews
    };
}
