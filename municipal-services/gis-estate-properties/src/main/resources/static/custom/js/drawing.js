////import { draw, flagIsDrawingOn, selectedGeomType } from './globals.js';
////import { drawSource } from './layers.js';
//
//window.app = {};
//var app = window.app;
//
///**
// * Custom drawing control button to start/stop drawing interaction.
// * @constructor
// * @extends {ol.control.Control}
// * @param {Object=} opt_options Control options.
// */
//app.DrawingApp = function(opt_options) {
//    var options = opt_options || {};
//
//    // Create a button element for the draw tool
//    var button = document.createElement('button');
//    button.id = 'drawbtn';
//    button.innerHTML = '<i class="fas fa-pencil-ruler"></i>'; // FontAwesome icon
//
//    var this_ = this;  // Reference to current object for event binding
//
//    // Function to start or stop drawing mode
//    var startStopApp = function() {
//        if (!flagIsDrawingOn) {
//            // If drawing is not active, show the modal to start drawing
//            $('#startdrawModal').modal('show');
//        } else {
//            // Stop drawing interaction
//            map.removeInteraction(draw);
//            flagIsDrawingOn = false;
//            document.getElementById('drawbtn').innerHTML = '<i class="fas fa-pencil-ruler"></i>';
//            defineTypeofFeature(); // Function to define the type of feature being drawn
//            $("#enterInformationModal").modal('show'); // Show the modal to enter additional details
//        }
//    };
//
//    // Attach event listeners to the button for both click and touch interactions
//    button.addEventListener('click', startStopApp, false);
//    button.addEventListener('touchstart', startStopApp, false);
//
//    // Create a container div for the control button
//    var element = document.createElement('div');
//    element.className = 'draw-app ol-unselectable ol-control';
//    element.appendChild(button);
//
//    // Extend OpenLayers control with this custom button
//    ol.control.Control.call(this, {
//        element: element,
//        target: options.target
//    });
//};
//
//// Inherit from OpenLayers control class
//ol.inherits(app.DrawingApp, ol.control.Control);
//ol.proj.useGeographic();
//
//
//// ===================== FUNCTION TO START DRAWING =====================
///**
// * Starts the drawing interaction based on the selected geometry type.
// * @param {string} geomType - The type of geometry to draw (Point, LineString, Polygon).
// */
//function startDraw(geomType){
//    selectedGeomType = geomType;  // Store the selected geometry type
//    draw = new ol.interaction.Draw({
//        type: geomType,  // Set the geometry type for drawing
//        source: drawSource  // Source for drawn features
//    });
//    $('#startdrawModal').modal('hide');  // Hide the start drawing modal
//
//    // Add the draw interaction to the map
//    map.addInteraction(draw);
//    flagIsDrawingOn = true;  // Mark drawing as active
//    document.getElementById('drawbtn').innerHTML = '<i class="far fa-stop-circle"></i>';  // Change the button to show stop icon
//}
//
//// ===================== FUNCTION TO DEFINE FEATURE TYPE =====================
///**
// * Updates the dropdown options for feature types based on the selected geometry type.
// */
//function defineTypeofFeature(){
//    var dropdownoftype = document.getElementById('typeofFeatures');  // Get the dropdown element
//    dropdownoftype.innerHTML = '';  // Clear the current dropdown options
//
//    // Populate dropdown options based on geometry type
//    if (selectedGeomType == 'Point'){
//        for (i = 0; i < PointType.length; i++) {
//            var op = document.createElement('option');
//            op.value = PointType[i];
//            op.innerHTML = PointType[i];
//            dropdownoftype.appendChild(op);  // Append options for point features
//        }
//    } else if (selectedGeomType == 'LineString') {
//        for (i = 0; i < LineType.length; i++) {
//            var op = document.createElement('option');
//            op.value = LineType[i];
//            op.innerHTML = LineType[i];
//            dropdownoftype.appendChild(op);  // Append options for line features
//        }
//    } else {
//        for (i = 0; i < PolygonType.length; i++) {
//            var op = document.createElement('option');
//            op.value = PolygonType[i];
//            op.innerHTML = PolygonType[i];
//            dropdownoftype.appendChild(op);  // Append options for polygon features
//        }
//    }
//}
//
////export { app, startDraw };
