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

//export { geolocation };
