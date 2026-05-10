function renderPublicNews(newsItems) {
    const container = document.getElementById("newsList");
    if (!container) {
        return;
    }

    if (!newsItems.length) {
        container.innerHTML = `<article class="news-public-card"><h2>Новостей пока нет</h2><p>Когда администратор или модератор опубликует первую новость, она появится здесь.</p></article>`;
        return;
    }

    container.innerHTML = newsItems.map((item) => `
        <article class="news-public-card">
            <div class="news-public-card__meta">
                <span class="badge badge_role">${escapeHtml(formatDateTime(item.updatedAt || item.createdAt))}</span>
            </div>
            <h2>${escapeHtml(item.title)}</h2>
            <p class="news-public-card__summary">${escapeHtml(item.summary)}</p>
            <div class="news-public-card__content">${escapeHtml(item.content).replaceAll("\n", "<br>")}</div>
        </article>
    `).join("");
}

async function loadNewsPage() {
    try {
        try {
            const sessionUser = await loadSessionUser();
            setupPageHeader(sessionUser);
        } catch {
            setupGuestPageHeader();
        }

        const newsItems = await pageRequest("/api/news/public");
        renderPublicNews(newsItems);
        clearPageError();
    } catch (error) {
        setPageError(extractErrorMessage(error, "Не удалось загрузить новости."));
    }
}

loadNewsPage();
