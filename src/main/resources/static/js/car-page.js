async function initCarPage() {
    try {
        const user = await loadSessionUser();
        setupPageHeader(user);
    } catch {
        setupGuestPageHeader();
    }
}

initCarPage();

