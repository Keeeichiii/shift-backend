const pickupMarkers = [
    [53.6895, 23.8251],
    [53.6846, 23.8383],
    [53.6804, 23.8477],
    [53.6768, 23.8289],
    [53.6917, 23.8545],
    [53.6712, 23.8148],
    [53.6635, 23.7919],
    [53.6548, 23.7645],
    [53.6461, 23.7762],
    [53.6998, 23.8715],
    [53.7052, 23.8216],
    [53.6751, 23.7934]
];

function initPickupMapPreview() {
    const mapNode = document.getElementById("pickupMap");
    if (!mapNode || typeof L === "undefined") {
        return;
    }

    const map = L.map(mapNode, {
        zoomControl: true,
        attributionControl: false,
        dragging: true,
        scrollWheelZoom: true,
        doubleClickZoom: true,
        boxZoom: true,
        keyboard: true,
        tap: false
    }).setView([53.684, 23.835], 12);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 18
    }).addTo(map);

    L.circle([53.6837, 23.8346], {
        radius: 6200,
        color: "#8b5cf6",
        weight: 2,
        fillColor: "#8b5cf6",
        fillOpacity: 0.12
    }).addTo(map);

    const carIcon = L.divIcon({
        className: "",
        html: "<div class=\"pickup-car-marker\"></div>",
        iconSize: [16, 16],
        iconAnchor: [8, 8]
    });

    pickupMarkers.forEach((coords, index) => {
        L.marker(coords, { icon: carIcon })
            .addTo(map)
            .bindPopup(`Машина ${index + 1}`);
    });
}

initPickupMapPreview();
