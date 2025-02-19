// ===================== GLOBAL VARIABLES =====================
var draw;  // Variable to store the draw interaction
var flagIsDrawingOn = false;  // Flag to track whether drawing mode is active

// Define feature types for different geometries
var PointType = ['ATM', 'TREE'];
var LineType = ['Telephone', 'Electricity'];
var PolygonType = ['Parks', 'Office Spaces', 'Commercial Land', 'Residential Land', 'Open Land'];

var selectedGeomType;  // Variable to store the selected geometry type


// ===================== CUSTOM POPUP =====================
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

// ===================== CUSTOM CONTROL =====================
window.app = {};
var app = window.app;

/** Custom drawing control button to start/stop drawing interaction. */
app.DrawingApp = function(opt_options) {
    var options = opt_options || {};

    // Create a button element for the draw tool
    var button = document.createElement('button');
    button.id = 'drawbtn';
    button.innerHTML = '<i class="fas fa-pencil-ruler"></i>'; // FontAwesome icon

    var this_ = this;  // Reference to current object for event binding

    // Function to start or stop drawing mode
    var startStopApp = function() {
        if (!flagIsDrawingOn) {
            // If drawing is not active, show the modal to start drawing
            $('#startdrawModal').modal('show');
        } else {
            // Stop drawing interaction
            map.removeInteraction(draw);
            flagIsDrawingOn = false;
            document.getElementById('drawbtn').innerHTML = '<i class="fas fa-pencil-ruler"></i>';
            defineTypeofFeature(); // Function to define the type of feature being drawn
            $("#enterInformationModal").modal('show'); // Show the modal to enter additional details
        }
    };

    // Attach event listeners to the button for both click and touch interactions
    button.addEventListener('click', startStopApp, false);
    button.addEventListener('touchstart', startStopApp, false);

    // Create a container div for the control button
    var element = document.createElement('div');
    element.className = 'draw-app ol-unselectable ol-control';
    element.appendChild(button);

    // Extend OpenLayers control with this custom button
    ol.control.Control.call(this, {
        element: element,
        target: options.target
    });
};

// Inherit from OpenLayers control class
ol.inherits(app.DrawingApp, ol.control.Control);
ol.proj.useGeographic();

// ===================== MAP INITIALIZATION =====================
// Create a new map view with predefined center and zoom
var myview = new ol.View({
    center: [77.2197, 28.5931], // Coordinates for India (Longitude, Latitude)
    zoom: 14, // Initial zoom level
    projection: 'EPSG:4326' // Set projection to WGS 84 (Latitude/Longitude)
});

// ===================== BASE LAYER (OSM) =====================
// OpenStreetMap (OSM) base layer
var baseLayer = new ol.layer.Tile({
    title: 'OSM Base Map',
    type: 'base',
    source: new ol.source.OSM({
        attributions: 'L&E GIS' // Custom attribution
    }),
    visible: true
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

//var googleSatellite = new ol.layer.Tile({
//    title: 'Google Satellite',
//    type: 'base',
//    visible: false,
//    source: new ol.source.XYZ({
//        url: "https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}&key=YOUR_GOOGLE_API_KEY",
//        attributions: '&copy; Google Maps'
//    })
//});

// Stamen Terrain (Free Terrain Map)
//var stamenTerrain = new ol.layer.Tile({
//    title: 'Google Terrain',
//    type: 'base',
//    visible: false,
//    source: new ol.source.XYZ({
//       url: "https://mt1.google.com/vt/lyrs=p&x={x}&y={y}&z={z}&key=YOUR_GOOGLE_API_KEY",
//       attributions: '&copy; Google Maps'
//    })
//});

// ===================== GEOSERVER LAYER =====================
var featureLayer = new ol.layer.Image({
    title: 'Feature Map Layer',
    type: 'overlay',
    visible: true,
    source: new ol.source.ImageWMS({
        url: 'http://13.127.171.159:8080/geoserver/wms', // GeoServer URL
        params: { 'LAYERS': 'LN_Estates_DB:Features', 'TILED': true, 'INFO_FORMAT': 'application/json' },
        ratio: 1,
        serverType: 'geoserver'
    })
});

// ===================== DRAW VECTOR LAYER =====================
// 1. Define a source to store drawn features
var drawSource = new ol.source.Vector();
// 2. Create a vector layer for user-drawn features
var drawLayer = new ol.layer.Vector({
    title: 'Drawn Map Base',
    type: 'overlay',
    visible: true,
    source: drawSource
});


// ===================== GROUP LAYERS =====================
// Group Base Layers (Only one can be active at a time)
var baseLayerGroup = new ol.layer.Group({
    title: "Base Layers (Select One)",
    layers: [baseLayer, esriSatellite, featureLayer, drawLayer].filter(layer => layer.get('title'))
});

// ===================== MAP CREATION =====================
// Initialize the OpenLayers map
var map = new ol.Map({
    // Add default controls and extend with the custom drawing control
    controls: ol.control.defaults({
        attributionOptions: {
            collapsible: false // Keep attribution visible
        }
    }).extend([
        new app.DrawingApp() // Add custom drawing button
    ]),
    target: 'map', // The ID of the HTML element where the map will be rendered
    view: myview, // Set the defined view
    layers: [baseLayerGroup], // Add layers to the map
    overlays: [popup] // Attach the popup overlay to the map
});

//setTimeout(() => {
//    restoreLayersFromLocalStorage();
//}, 1000);

// Log all layers in the map for debugging
console.log("All Layers in Map:", map.getLayers().getArray());


// ===================== FUNCTION TO START DRAWING =====================
/**
 * Starts the drawing interaction based on the selected geometry type.
 * @param {string} geomType - The type of geometry to draw (Point, LineString, Polygon).
 */
function startDraw(geomType){
    selectedGeomType = geomType;  // Store the selected geometry type
    draw = new ol.interaction.Draw({
        type: geomType,  // Set the geometry type for drawing
        source: drawSource  // Source for drawn features
    });
    $('#startdrawModal').modal('hide');  // Hide the start drawing modal

    // Add the draw interaction to the map
    map.addInteraction(draw);
    flagIsDrawingOn = true;  // Mark drawing as active
    document.getElementById('drawbtn').innerHTML = '<i class="far fa-stop-circle"></i>';  // Change the button to show stop icon
}

// ===================== FUNCTION TO DEFINE FEATURE TYPE =====================
/**
 * Updates the dropdown options for feature types based on the selected geometry type.
 */
function defineTypeofFeature(){
    var dropdownoftype = document.getElementById('typeofFeatures');  // Get the dropdown element
    dropdownoftype.innerHTML = '';  // Clear the current dropdown options

    // Populate dropdown options based on geometry type
    if (selectedGeomType == 'Point'){
        for (i = 0; i < PointType.length; i++) {
            var op = document.createElement('option');
            op.value = PointType[i];
            op.innerHTML = PointType[i];
            dropdownoftype.appendChild(op);  // Append options for point features
        }
    } else if (selectedGeomType == 'LineString') {
        for (i = 0; i < LineType.length; i++) {
            var op = document.createElement('option');
            op.value = LineType[i];
            op.innerHTML = LineType[i];
            dropdownoftype.appendChild(op);  // Append options for line features
        }
    } else {
        for (i = 0; i < PolygonType.length; i++) {
            var op = document.createElement('option');
            op.value = PolygonType[i];
            op.innerHTML = PolygonType[i];
            dropdownoftype.appendChild(op);  // Append options for polygon features
        }
    }
}

// ===================== API BASE URL =====================
var baseUrl = window.location.origin + "/";  // Get the base URL of the current site
console.log("🌍 API Base URL:", baseUrl);  // Log the base URL for API calls





// ===================== FUNCTION TO CLEAR DRAWING SOURCE =====================
/**
 * Clears all drawn features from the map.
 */
function clearDrawSource (){
    drawSource.clear();  // Clear the draw source to remove features
}

// ===================== GEOLLOCATION SETUP =====================
// Set up geolocation to track user's position and update map view accordingly
var geolocation = new ol.Geolocation({
    tracking: true,  // Enable position tracking
    projection: map.getView().getProjection(),  // Bind to the current map projection
    enableHighAccuracy: true,  // Enable high accuracy for geolocation
});

// Update map center and marker as user moves
geolocation.on('change:position', function() {
    myview.setCenter(geolocation.getPosition());  // Update map center to current position
    addmarker(geolocation.getPosition());  // Add marker at the current position
});

// ===================== MARKER SETUP =====================
// Create a marker overlay to display the current location
var marker = new ol.Overlay({
    element: document.getElementById('currentLocation'),  // Use existing HTML element for the marker
    positioning: 'center-center',  // Position the marker at the center
});

// Add the marker overlay to the map
map.addOverlay(marker);

// Function to update the marker position
function addmarker(array){
    marker.setPosition(array);  // Set the new position for the marker
}

// ===================== DEVICE ORIENTATION =====================
// Handle device orientation events to rotate the map based on device heading
if (window.DeviceOrientationEvent) {
    window.addEventListener("deviceorientation", function(event) {
        if (event.alpha !== null) {
            var heading = event.alpha * (Math.PI / 180);  // Convert degrees to radians
            map.getView().setRotation(-heading);  // Set the map rotation based on device heading
            console.log("Device heading:", event.alpha);  // Log the device heading in degrees
        }
    });
} else {
    console.log("DeviceOrientation API not supported in this browser.");  // Log if DeviceOrientation is not supported
}

// Group all overlay layers
overlays = new ol.layer.Group({
    'title': 'Overlays',
    layers: []
});

// Add layer switcher control to the map
layerSwitcher = new ol.control.LayerSwitcher({
    activationMode: 'click',  // Enable switching layers on click
    startActive: true,  // Keep the layer switcher active by default
    tipLabel: 'Layers',  // Tooltip label for the button
    groupSelectStyle: 'group',  // Select children layers when a parent is clicked
    collapseTipLabel: 'Collapse layers',  // Tooltip label for collapsing the layers
});

// Add the layer switcher to the map
map.addControl(layerSwitcher);
//map.once('rendercomplete', updateLayerSwitcher);




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
                $('<tr class="btn-success text-white"></tr>').html('<th>Name</th><th>Title</th>').appendTo('#table_wms_layers');

                // Parse XML to extract WMS layer information
                $(xml).find('Layer').find('Layer').each(function() {
                    var name = $(this).children('Name').text();
                    var title = $(this).children('Title').text();
//                    var abst = $(this).children('Abstract').text();

                    $('<tr></tr>').html('<td>' + name + '</td><td>' + title + '</td>').appendTo('#table_wms_layers');
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
                            $(this).parent("tr").css("background-color", "lightgrey"); // Highlight selected row
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

    if (!name || name.trim() === "" || name === "undefined" || name === null) {
            console.warn("Skipping layer addition: Invalid or empty layer name.");
            return;
        }
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

    var url = 'http://13.127.171.159:8080/geoserver/wms?request=getCapabilities';
        var parser = new ol.format.WMSCapabilities();
        $.ajax(url).then(function(response) {
            //window.alert("word");
            var result = parser.read(response);
            // console.log(result);
            var Layers = result.Capability.Layer.Layer;
            var extent;
            for (var i = 0, len = Layers.length; i < len; i++) {
                var layerobj = Layers[i];
                //  window.alert(layerobj.Name);
                if (layerobj.Name == layer_name) {
                    extent = layerobj.BoundingBox[0].extent;
                    //alert(extent);
                    map.getView().fit(
                        extent, {
                            duration: 1590,
                            size: map.getSize()
                        }
                    );
                }
            }
        });
    legend();
    // Store in localStorage to persist after refresh
    //saveLayersToLocalStorage();
    console.log("Layer added:", name);
    //updateLayerSwitcher();
    layerSwitcher.renderPanel();  // Update LayerSwitcher

}



// ===================== FUNCTION TO CLOSE WMS LAYERS MODAL =====================
function close_wms_window() {
    layer_name = undefined;
}

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
            console.log(`📡 Requesting Feature Info for: ${queryLayer}`);

            var url = source.getFeatureInfoUrl(coord, resolution, projection, {
                'INFO_FORMAT': 'application/json',
                'QUERY_LAYERS': queryLayer,  // ✅ Corrected to dynamically query the right layer
                'FEATURE_COUNT': 5  // Increase feature count to ensure multiple features are captured
            });

            if (url) {
                console.log(`🌍 Feature Info Request URL: ${url}`);

                $.getJSON(url, function (data) {
                    console.log(`📊 Feature Info Retrieved for Layer: ${layer.get('title')}`, data);

                    // ✅ Prevent crash if `features` is missing or undefined
                    if (!data.features || data.features.length === 0) {
                        console.warn(`⚠ No features found at clicked location. Raw Response:`, data);
                        return;
                    }

                    // ✅ Extract the first feature
                    var feature = data.features[0];

                    // ✅ Ensure properties exist to avoid undefined errors
                    var gid = feature.properties?.gid || 'N/A';
                    var bungalowNo = feature.properties?.bungalow_n || 'N/A';
                    var type = feature.properties?.type || 'N/A';
                    var name = feature.properties?.name || 'N/A';

                    var content = `<b>GID</b>: ${gid} <br>
                                   <b>Bungalow No</b>: ${bungalowNo} <br>
                                   <b>Type</b>: ${type} <br>
                                   <b>Name</b>: ${name}`;

                    console.log(`🛠 Geometry Type: ${feature.geometry.type}`);
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

                    console.log(`Adjusted Popup Coordinates for ${layer.get('title')}:`, popupCoordinates);
                    popup.show(popupCoordinates, content);
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    console.error("GetFeatureInfo request failed:", textStatus, errorThrown);
                });
            }
        }
    });
});

// refresh layers
//setTimeout(() => {
//    var existingLayers = map.getLayers().getArray();
//    var featureLayerExists = existingLayers.some(layer => layer.get('title') === 'Feature Map Layer');
//
//    if (!featureLayerExists) {
//        map.addLayer(featureLayer);
//        console.log("Feature layer added to the map.");
//    } else {
//        console.log("Feature layer already exists, refreshing...");
//        var params = featureLayer.getSource().getParams();
//        params.t = new Date().getMilliseconds(); // Force refresh by adding timestamp
//        featureLayer.getSource().updateParams(params);
//    }
//}, 2000);  // Delay for better initialization

var mouse_position = new ol.control.MousePosition();
map.addControl(mouse_position);
var slider = new ol.control.ZoomSlider();
map.addControl(slider);

var zoom_ex = new ol.control.ZoomToExtent({
    extent: [
        65.90, 7.48,
        98.96, 40.30
    ]
});
map.addControl(zoom_ex);

var scale_line = new ol.control.ScaleLine({
    units: 'metric',
    bar: true,
    steps: 6,
    text: true,
    minWidth: 140,
    target: 'scale_bar'
});
map.addControl(scale_line);

//function saveLayersToLocalStorage() {
//    var currentLayers = overlaysGroup.getLayers().getArray()
//        .map(layer => layer.get('title'))
//        .filter(title => title !== undefined && title !== null);
//
//    // Remove duplicates using Set
//    var uniqueLayers = [...new Set(currentLayers)];
//
//    localStorage.setItem('dynamicLayers', JSON.stringify(uniqueLayers));
//
//    console.log("Stored Layers (No Duplicates):", uniqueLayers);
//}


//function restoreLayersFromLocalStorage() {
//    var savedLayers = JSON.parse(localStorage.getItem('dynamicLayers'));
//
//    if (savedLayers && savedLayers.length > 0) {
//        console.log("Restoring Layers:", savedLayers);
//        savedLayers.forEach(layer_name => {
//            var layer_wms = new ol.layer.Image({
//                title: layer_name,
//                source: new ol.source.ImageWMS({
//                    url: 'http://13.127.171.159:8080/geoserver/wms',
//                    params: { 'LAYERS': layer_name, 'TILED': true },
//                    ratio: 1,
//                    serverType: 'geoserver'
//                })
//            });
//
//            overlaysGroup.getLayers().push(layer_wms);
//            map.addLayer(layer_wms);
//        });
//
//        console.log("✅ All saved layers restored!");
//    } else {
//            console.log("⚠ No layers to restore from localStorage.");
//    }
//}

// ===================== FUNCTION TO SAVE INFORMATION IN DATABASE =====================
/**
 * Saves the drawn features and their information to the database via API.
 */
function savetodb() {
    console.log("Projection format: ");
    console.log(map.getView().getProjection().getCode());  // Log the map's current projection code

    // Get all the features drawn on the map
    var featureArray = drawSource.getFeatures();

    // Create a GeoJSON format object
    var geoJSONFormat = new ol.format.GeoJSON();

    // Convert the features to GeoJSON format
    var featuresGeojson = geoJSONFormat.writeFeaturesObject(featureArray);
    var geojsonFeatureArray = featuresGeojson.features;  // Extract features array from GeoJSON object

    // Loop through each GeoJSON feature and send data to the server
    for (let i = 0; i < geojsonFeatureArray.length; i++) {
        var type = document.getElementById('typeofFeatures').value;  // Get the selected feature type
        var name = document.getElementById('exampleInputtext1').value;  // Get the feature name
        var geom = JSON.stringify(geojsonFeatureArray[i].geometry);  // Get the geometry in GeoJSON format

        // Debugging: Log GeoJSON data before saving
        console.log("📌 Debugging GeoJSON Data before saving:");
        console.log("🔹 Geometry Type:", type);
        console.log("🔹 Feature Name:", name);
        console.log("🔹 Geometry (GeoJSON):", geom);

        // Only send data if a valid feature type is selected
        if (type !== '') {
            // Send POST request to save the feature to the database
            fetch('api/features/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    type: type,
                    name: name,
                    wktGeometry: convertToWKT(geom)  // Convert GeoJSON geometry to WKT format
                })
            })
            .then(response => response.json())
            .then(data => {
                if (data.id) {
                    console.log('Data added successfully:', data);  // Log success if data is added
                } else {
                    console.error('Data not added successfully:', data);  // Log error if data was not added
                }
            })
            .catch(error => console.error('API Call Error:', error));  // Handle any API call errors
        } else {
            alert('Please select a type.');  // Alert if no feature type is selected
        }
    }

    // Update the feature layer to reflect new data
    var params = featureLayer.getSource().getParams();
    params.t = new Date().getMilliseconds();  // Add timestamp to update layer
    featureLayer.getSource().updateParams(params);  // Update layer parameters

    // Close the modal after saving
    $("#enterInformationModal").modal('hide');

    // Clear the drawn features from the map
    clearDrawSource();
}

// ===================== FUNCTION TO CONVERT GEOJSON TO WKT FORMAT =====================
/**
 * Converts GeoJSON geometry to Well-Known Text (WKT) format.
 * @param {string} geoJSON - GeoJSON string of the geometry.
 * @returns {string} - WKT representation of the geometry.
 */
function convertToWKT(geoJSON) {
    var geoObj = JSON.parse(geoJSON);  // Parse the GeoJSON string to an object
    if (geoObj.type === "Point") {
        return `POINT(${geoObj.coordinates.join(" ")})`;  // Convert Point to WKT
    } else if (geoObj.type === "Polygon") {
        return `POLYGON((${geoObj.coordinates[0].map(coord => coord.join(" ")).join(", ")}))`;  // Convert Polygon to WKT
    } else if (geoObj.type === "LineString") {
        return `LINESTRING(${geoObj.coordinates.map(coord => coord.join(" ")).join(", ")})`;  // Convert LineString to WKT
    }
    return "";  // Return empty string if unsupported geometry type
}


//function updateLayerSwitcher() {
//    setTimeout(() => {
//        document.querySelectorAll('.ol-layerswitcher label').forEach((label, index) => {
//            var layer = map.getLayers().item(index);
//            if (!layer || !layer.get('title') || layer.get('title').trim() === "" || layer.get('title') === "undefined") {
//                            console.warn("⚠ Removing unnamed layer from switcher.");
//                            label.parentNode.style.display = 'none'; // Hide unnamed layers
//                        }
//        });
//    }, 500);
//}

localStorage.clear();
sessionStorage.clear();