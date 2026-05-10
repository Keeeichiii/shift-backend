const freeZones = [
    {
        name: "Городская зона Гродно",
        description: "Бесплатное завершение аренды внутри основной городской зоны.",
        center: [53.6837, 23.8346],
        radius: 6200,
        color: "#8b5cf6"
    }
];

const paidZones = [
    {
        name: "Аэропорт Гродно",
        description: "Платная зона завершения аренды.",
        fee: "15 BYN",
        center: [53.6039, 24.0532],
        radius: 850,
        color: "#ef4444"
    },
    {
        name: "Коробчицы",
        description: "Выездная зона около туристического комплекса.",
        fee: "8 BYN",
        center: [53.6025, 23.7838],
        radius: 900,
        color: "#ef4444"
    },
    {
        name: "Понемунь",
        description: "Платная зона завершения аренды у западной части города.",
        fee: "5 BYN",
        center: [53.6507, 23.7424],
        radius: 700,
        color: "#ef4444"
    },
    {
        name: "Ольшанка",
        description: "Отдельная зона завершения аренды в новом районе.",
        fee: "4 BYN",
        center: [53.6396, 23.7634],
        radius: 850,
        color: "#ef4444"
    },
    {
        name: "ЖД вокзал",
        description: "Платная зона у транспортного узла.",
        fee: "3 BYN",
        center: [53.6776, 23.8213],
        radius: 550,
        color: "#ef4444"
    }
];

function renderZoneList(containerId, zones, type) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = zones.map((zone) => `
        <article class="zone-item zone-item_${type}">
            <div>
                <strong>${escapeHtml(zone.name)}</strong>
                <p>${escapeHtml(zone.description)}</p>
            </div>
            <span>${escapeHtml(zone.fee || "Бесплатно")}</span>
        </article>
    `).join("");
}

function initMap() {
    const mapNode = document.getElementById("grodnoMap");
    if (!mapNode || typeof L === "undefined") {
        if (mapNode) {
            mapNode.innerHTML = "<div class=\"map-fallback\">Карта временно недоступна.</div>";
        }
        return;
    }

    const map = L.map(mapNode, {
        zoomControl: true,
        scrollWheelZoom: true
    }).setView([53.684, 23.835], 12);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 18,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    const bounds = [];

    freeZones.forEach((zone) => {
        const circle = L.circle(zone.center, {
            radius: zone.radius,
            color: zone.color,
            weight: 2,
            fillColor: zone.color,
            fillOpacity: 0.18
        }).addTo(map);
        circle.bindPopup(`<strong>${escapeHtml(zone.name)}</strong><br>${escapeHtml(zone.description)}`);
        bounds.push(circle.getBounds());
    });

    paidZones.forEach((zone) => {
        const circle = L.circle(zone.center, {
            radius: zone.radius,
            color: zone.color,
            weight: 2,
            fillColor: zone.color,
            fillOpacity: 0.18
        }).addTo(map);
        circle.bindPopup(
            `<strong>${escapeHtml(zone.name)}</strong><br>${escapeHtml(zone.description)}<br><b>${escapeHtml(zone.fee)}</b>`
        );
        bounds.push(circle.getBounds());
    });

    if (bounds.length) {
        const featureGroup = L.featureGroup(bounds.map((bound) => L.rectangle(bound)));
        map.fitBounds(featureGroup.getBounds().pad(0.12));
    }
}

async function initMapPage() {
    try {
        const user = await loadSessionUser();
        setupPageHeader(user);
    } catch {
        setupGuestPageHeader();
    }

    renderZoneList("freeZoneList", freeZones, "free");
    renderZoneList("paidZoneList", paidZones, "paid");
    initMap();
}

initMapPage();

