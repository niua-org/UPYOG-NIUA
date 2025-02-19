//import { app } from './drawing.js';
//import { baseLayerGroup, overlaysGroup } from './layers.js';
//import { popup } from './popups.js';

// ===================== MAP INITIALIZATION =====================
// Create a new map view with predefined center and zoom
var myview = new ol.View({
    center: [77.2197, 28.5931],
    zoom: 14,
    // Set projection to WGS 84 (Latitude/Longitude)
    projection: 'EPSG:4326'
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
    target: 'mymap', // The ID of the HTML element where the map will be rendered
    view: myview, // Set the defined view
    layers: [baseLayerGroup, overlaysGroup], // Add layers to the map
    overlays: [popup] // Attach the popup overlay to the map
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

// ===================== FUNCTION TO CLOSE WMS LAYERS MODAL =====================
function close_wms_window() {
    layer_name = undefined;
}

// refresh layers
setTimeout(() => {
    var existingLayers = map.getLayers().getArray();
    var featureLayerExists = existingLayers.some(layer => layer.get('title') === 'Feature Map Layer');

    if (!featureLayerExists) {
        map.addLayer(featureLayer);
        console.log("Feature layer added to the map.");
    } else {
        console.log("Feature layer already exists, refreshing...");
        var params = featureLayer.getSource().getParams();
        params.t = new Date().getMilliseconds(); // Force refresh by adding timestamp
        featureLayer.getSource().updateParams(params);
    }
}, 2000);  // Delay for better initialization


//export { map, addmarker, close_wms_window };