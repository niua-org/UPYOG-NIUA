const LeafletMethods = {
    initializeMap: function () {
        const map = L.map('map').setView([28.6139, 77.2090], 11);

        const osm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 20,
            attribution: '&copy; OpenStreetMap contributors',
        }).addTo(map);

        const satellite = L.tileLayer('https://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
            subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
            attribution: '&copy; Google Satellite',
        });

        const terrain = L.tileLayer('https://{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}', {
            subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
            attribution: '&copy; Google Terrain',
        });

        const circle = L.circle([28.6139, 77.2090], {
            color: '#e6f7ff',
            fillColor: '#009999',
            fillOpacity: 0.1,
            radius: 7000,
        }).bindPopup('<b>ULB Delhi, India</b>').addTo(map);

        const point_layers = L.layerGroup().addTo(map);

        L.control.layers(
            {
                'OpenStreetMap': osm,
                'Satellite': satellite,
                'Terrain': terrain,
            },
            {
                'Properties Marker (Delhi)': point_layers,
                'Red Circle': circle,
            }
        ).addTo(map);

        return { map, point_layers };
    },

    addMarkersToMap: function (map, point_layers, dataUrl) {
        $.getJSON(dataUrl, function (data) {
            data.forEach(item => {
                const lat = parseFloat(item.latitude);
                const lng = parseFloat(item.longitude);

                if (!isNaN(lat) && !isNaN(lng)) {
                    const marker = L.marker([lat, lng]).addTo(map);
                    marker.bindPopup(
                        `<h4><b>Bungalow No:</b> ${item.bungalowNo}</h4>
                        <h4><b>Name:</b> ${item.name}</h4>
                        <button class='btn btn-link edit-button' style='padding: 0; color: blue;' data-id='${item.id}'>
                            <i class='fa fa-edit'></i> Edit
                        </button>
                        <form id='editForm-${item.id}' class='form' style='display: none; margin-top: 10px;'>
                            <div class='form-group' style='width: 100%;'>
                                <label for='bungalowNo-${item.id}'><b>Bungalow No:</b></label>
                                <input type='text' class='form-control' id='bungalowNo-${item.id}' value='${item.bungalowNo}' />
                            </div>
                            <div class='form-group' style='width: 100%;'>
                                <label for='name-${item.id}'><b>Name:</b></label>
                                <input type='text' class='form-control' id='name-${item.id}' value='${item.name}' />
                            </div>
                            <div class='form-group' style='width: 100%; text-align: right;'>
                                <button type='submit' class='btn btn-primary'>Save</button>
                            </div>
                        </form>`
                    );
                    point_layers.addLayer(marker);
                } else {
                    console.error(`Invalid coordinates for marker: ${item}`);
                }
            });
        });
    },

    initializeDrawControl: function (map, drawnItems) {
        const drawControl = new L.Control.Draw({
            edit: {
                featureGroup: drawnItems,
            },
            draw: {
                polygon: true,
                rectangle: true,
                circle: false,
                marker: false,
            },
        });
        map.addControl(drawControl);

        map.on('draw:created', function (event) {
            const layer = event.layer;
            drawnItems.addLayer(layer);

            const geojson = layer.toGeoJSON();
            console.log('New shape GeoJSON:', geojson);

            fetch('/api/saveShape', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(geojson),
            })
                .then(response => {
                    if (response.ok) {
                        console.log('Shape saved successfully');
                    } else {
                        console.error('Failed to save shape');
                    }
                })
                .catch(error => console.error('Error saving shape:', error));
        });
    },

    initialize: function () {
        const { map, point_layers } = LeafletMethods.initializeMap();

        const dataUrl = "http://localhost:8084/properties/api/all";
        LeafletMethods.addMarkersToMap(map, point_layers, dataUrl);

        const drawnItems = new L.FeatureGroup();
        map.addLayer(drawnItems);
        LeafletMethods.initializeDrawControl(map, drawnItems);
    },
};

// Event listener for dynamically added "edit-button"
$(document).on('click', '.edit-button', function () {
    const id = $(this).data('id'); // Get the ID from the data-id attribute
    const form = document.getElementById(`editForm-${id}`); // Find the form by ID

    if (!form) {
        console.error(`Form with id 'editForm-${id}' not found.`);
        return;
    }

    // Toggle form visibility
    form.style.display = form.style.display === "none" ? "block" : "none";
    console.log(`Toggled form visibility for ID: ${id}`);
});

// Initialize when the document is ready
$(document).ready(function () {
    LeafletMethods.initialize();
});
