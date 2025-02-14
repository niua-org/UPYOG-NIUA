// ===================== CUSTOM POPUP =====================
// Define a popup overlay with custom styling and behavior
var popup = new ol.Overlay.Popup({
    popupClass: "default anim", // Available classes: "tooltips", "warning", "black", "default", "tips", "shadow"
    closeBox: true,  // Adds a close button to the popup
    onclose: function() {
        console.log("You closed the popup box");
    },
    positioning: 'auto',  // Auto-positioning of popup
    autoPan: true,  // Automatically pans the map to ensure popup visibility
    autoPanAnimation: { duration: 100 }  // Smooth animation for auto panning
});

// ===================== FUNCTION TO GET FEATURE INFO ON CLICK =====================
/**
 * Retrieves feature information on map click and displays it in a popup.
 */
map.on('click', function(evt) {
    popup.hide();  // Hide any existing popups
    var resolution = map.getView().getResolution();
    var coord = evt.coordinate;
    var projection = map.getView().getProjection();

    console.log("🔍 Checking all overlay layers for GetFeatureInfo...");

    overlays.getLayers().getArray().forEach(function (layer) {
        var source = layer.getSource();

        if (source instanceof ol.source.TileWMS || source instanceof ol.source.ImageWMS) {
            // ✅ Dynamically get the correct layer name
            var queryLayer = source.getParams().LAYERS;
            console.log(📡 Requesting Feature Info for: ${queryLayer});

            var url = source.getFeatureInfoUrl(coord, resolution, projection, {
                'INFO_FORMAT': 'application/json',
                'QUERY_LAYERS': queryLayer,  // ✅ Corrected to dynamically query the right layer
                'FEATURE_COUNT': 5  // Increase feature count to ensure multiple features are captured
            });

            if (url) {
                console.log(🌍 Feature Info Request URL: ${url});

                $.getJSON(url, function (data) {
                    console.log(📊 Feature Info Retrieved for Layer: ${layer.get('title')}, data);

                    // ✅ Prevent crash if features is missing or undefined
                    if (!data.features || data.features.length === 0) {
                        console.warn(⚠ No features found at clicked location. Raw Response:, data);
                        return;
                    }

                    // ✅ Extract the first feature
                    var feature = data.features[0];

                    // ✅ Ensure properties exist to avoid undefined errors
                    var gid = feature.properties?.gid || 'N/A';
                    var bungalowNo = feature.properties?.bungalow_n || 'N/A';
                    var type = feature.properties?.type || 'N/A';
                    var name = feature.properties?.name || 'N/A';

                    var content = <b>GID</b>: ${gid} <br>
                                   <b>Bungalow No</b>: ${bungalowNo} <br>
                                   <b>Type</b>: ${type} <br>
                                   <b>Name</b>: ${name};

                    console.log(🛠 Geometry Type: ${feature.geometry.type});
                    console.log("📍 Raw Coordinates:", feature.geometry.coordinates);

                    // ✅ Improved popup coordinate selection
                    let popupCoordinates;
                    switch (feature.geometry.type) {
                        case 'Polygon':
                            popupCoordinates = feature.geometry.coordinates[0][0];
                            break;
                        case 'Point':
                            popupCoordinates = feature.geometry.coordinates;
                            break;
                        case 'MultiPolygon':
                            popupCoordinates = feature.geometry.coordinates[0][0][0];
                            break;
                        case 'LineString':
                            popupCoordinates = feature.geometry.coordinates[0];
                            break;
                        default:
                            popupCoordinates = coord;  // Default fallback
                    }

                    console.log(Adjusted Popup Coordinates for ${layer.get('title')}:, popupCoordinates);
                    popup.show(popupCoordinates, content);
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    console.error("GetFeatureInfo request failed:", textStatus, errorThrown);
                });
            }
        }
    });
});

// ✅ Add event listener for map clicks to call `getFeatureInfo`
map.on('click', getFeatureInfo);

// ✅ Export the popup & feature info function
//export { popup, getFeatureInfo };
