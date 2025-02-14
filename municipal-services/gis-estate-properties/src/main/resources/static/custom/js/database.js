//import { drawSource } from './layers.js';

/// ===================== FUNCTION TO SAVE INFORMATION IN DATABASE =====================
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

/**
 * Converts GeoJSON geometry to Well-Known Text (WKT) format.
 * @param {string} geoJSON - GeoJSON string of the geometry.
 * @returns {string} - WKT representation of the geometry.
 */
function convertToWKT(geoJSON) {
    var geoObj = JSON.parse(geoJSON);
    if (geoObj.type === "Point") {
        return `POINT(${geoObj.coordinates.join(" ")})`;
    } else if (geoObj.type === "Polygon") {
        return `POLYGON((${geoObj.coordinates[0].map(coord => coord.join(" ")).join(", ")}))`;
    } else if (geoObj.type === "LineString") {
        return `LINESTRING(${geoObj.coordinates.map(coord => coord.join(" ")).join(", ")})`;
    }
    return "";
}

/**
 * Clears all drawn features from the map.
 */
function clearDrawSource() {
    drawSource.clear();
}

//export { savetodb };
