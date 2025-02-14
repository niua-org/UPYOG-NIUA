// ===================== BASE LAYER (OSM) =====================
// OpenStreetMap (OSM) base layer
var baseLayer = new ol.layer.Tile({
    title: 'OSM Base Map',
    type: 'base',
    source: new ol.source.OSM({
        attributions: 'L&E GIS' // Custom attribution
    })
});

// ESRI Satellite Imagery (Free Alternative to Google Satellite)
var esriSatellite = new ol.layer.Tile({
    title: 'ESRI Satellite',
    type: 'base',
    visible: false,
    source: new ol.source.XYZ({
        url: "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    })
});

// Stamen Terrain (Free Terrain Map)
var stamenTerrain = new ol.layer.Tile({
//    title: "Stamen Terrain",
//    type: "base",
//    visible: false,
//    source: new ol.source.XYZ({
//        url: "http://stamen-tiles.a.ssl.fastly.net/terrain/{z}/{x}/{y}.jpg"
//    })
     title: "Terrain Map",
        type: "base",
        visible: false,
        source: new ol.source.XYZ({
            url: "https://{a-c}.tile.opentopomap.org/{z}/{x}/{y}.png",
            attributions: [
                '&copy; <a href="https://opentopomap.org">OpenTopoMap</a>',
                '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
            ]
        })
});

// ===================== GEOSERVER LAYER =====================
// Source for GeoServer WMS layer
var featureLayerSource = new ol.source.ImageWMS({
    url: 'http://13.127.171.159:8080/geoserver/wms', // GeoServer URL
    params: { 'LAYERS': 'LN_Estates_DB:features', 'TILED': false, 'INFO_FORMAT': 'application/json', 'gutter': 50, 'SRS': 'EPSG:4326' }, // Layer parameters
    serverType: 'geoserver' // Specify server type
});

// Layer to visualize features from GeoServer
var featureLayer = new ol.layer.Image({
    title: 'Feature Map Layer',
    type: 'base',
    source: featureLayerSource
});



// ===================== DRAW VECTOR LAYER =====================
// 1. Define a source to store drawn features
var drawSource = new ol.source.Vector();

// 2. Create a vector layer for user-drawn features
var drawLayer = new ol.layer.Vector({
    title: 'Drawn Map Base',
    type: 'base',
    source: drawSource
});


// ===================== GROUP LAYERS =====================
// Group Base Layers (Only one can be active at a time)
var baseLayerGroup = new ol.layer.Group({
    title: "Base Layers (Select One)",
    layers: [baseLayer, esriSatellite, stamenTerrain]
});

// Group Overlays (Multiple can be active)
var overlaysGroup = new ol.layer.Group({
    title: "Overlays (Multiple Allowed)",
    layers: [featureLayer, drawLayer]
});

// ===================== FUNCTION TO DISPLAY WMS LAYERS IN MODAL =====================
/**
 * Opens a modal window displaying available WMS layers from GeoServer.
 */
function wms_layers() {
    $(function() {
        $("#wms_layers_window").modal({ backdrop: false });
        $("#wms_layers_window").draggable();
        $("#wms_layers_window").modal('show');
    });

    $(document).ready(function() {
        $.ajax({
            type: "GET",
            url: "http://13.127.171.159:8080/geoserver/wms?request=getCapabilities",
            dataType: "xml",
            success: function(xml) {
                $('#table_wms_layers').empty();
                $('<tr></tr>').html('<th>Name</th><th>Title</th><th>Abstract</th>').appendTo('#table_wms_layers');

                // Parse XML to extract WMS layer information
                $(xml).find('Layer').find('Layer').each(function() {
                    var name = $(this).children('Name').text();
                    var title = $(this).children('Title').text();
                    var abst = $(this).children('Abstract').text();

                    $('<tr></tr>').html('<td>' + name + '</td><td>' + title + '</td><td>' + abst + '</td>').appendTo('#table_wms_layers');
                });

                addRowHandlers1();  // Attach event handlers for table rows
            }
        });
    });

    function addRowHandlers1() {
        var rows = document.getElementById("table_wms_layers").rows;
        var table = document.getElementById('table_wms_layers');
        var heads = table.getElementsByTagName('th');
        var col_no;

        // Identify the column index for "Name"
        for (var i = 0; i < heads.length; i++) {
            if (heads[i].innerHTML == 'Name') {
                col_no = i + 1;
            }
        }

        for (i = 0; i < rows.length; i++) {
            rows[i].onclick = function() {
                return function() {
                    $("#table_wms_layers td").parent("tr").css("background-color", "white"); // Reset row color

                    var cell = this.cells[col_no - 1];
                    layer_name = cell.innerHTML;

                    $("#table_wms_layers td:nth-child(" + col_no + ")").each(function() {
                        if ($(this).text() == layer_name) {
                            $(this).parent("tr").css("background-color", "grey"); // Highlight selected row
                        }
                    });
                };
            }(rows[i]);
        }
    }
}

// ===================== FUNCTION TO ADD SELECTED WMS LAYER TO MAP =====================
/**
 * Adds a selected WMS layer from the modal to the map.
 */
function add_layer() {
    if (!layer_name) {
        alert("No WMS layer selected.");
        return;
    }

    var name = layer_name.split(":")[1] || layer_name; // Extract layer name if namespace exists

    // Prevent duplicate layer addition
    var existingLayers = overlays.getLayers().getArray();
    for (var i = 0; i < existingLayers.length; i++) {
        if (existingLayers[i].get('title') === name) {
            alert("Layer already added to the map.");
            return;
        }
    }

    var layer_wms = new ol.layer.Image({
        title: name,
        source: new ol.source.ImageWMS({
            url: 'http://13.127.171.159:8080/geoserver/wms',
            params: { 'LAYERS': layer_name, 'TILED': true },
            ratio: 1,
            serverType: 'geoserver'
        })
    });

    overlays.getLayers().push(layer_wms);
    map.addLayer(layer_wms);
    legend();  // Refresh legend after adding the layer
    // Store in localStorage to persist after refresh
    saveLayersToLocalStorage();
    console.log("Layer added:", name);

    layerSwitcher.renderPanel();  // Update LayerSwitcher


}

function saveLayersToLocalStorage() {
    var currentLayers = overlaysGroup.getLayers().getArray()
        .map(layer => layer.get('title'))
        .filter(title => title !== undefined && title !== null);

    // Remove duplicates using Set
    var uniqueLayers = [...new Set(currentLayers)];

    localStorage.setItem('dynamicLayers', JSON.stringify(uniqueLayers));

    console.log("Stored Layers (No Duplicates):", uniqueLayers);
}


function restoreLayersFromLocalStorage() {
    var savedLayers = JSON.parse(localStorage.getItem('dynamicLayers'));

    if (savedLayers && savedLayers.length > 0) {
        console.log("Restoring Layers:", savedLayers);
        savedLayers.forEach(layer_name => {
            var layer_wms = new ol.layer.Image({
                title: layer_name,
                source: new ol.source.ImageWMS({
                    url: 'http://13.127.171.159:8080/geoserver/wms',
                    params: { 'LAYERS': layer_name, 'TILED': true },
                    ratio: 1,
                    serverType: 'geoserver'
                })
            });

            overlaysGroup.getLayers().push(layer_wms);
            map.addLayer(layer_wms);
        });

        console.log("✅ All saved layers restored!");
    } else {
            console.log("⚠ No layers to restore from localStorage.");
    }
}


//export { baseLayerGroup, overlaysGroup, drawLayer, drawSource, wms_layers, add_layer, saveLayersToLocalStorage, restoreLayersFromLocalStorage };
