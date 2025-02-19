// ===================== FUNCTION TO SHOW/HIDE LEGEND =====================
/**
 * Toggles the visibility of the legend panel on the map.
 */
function show_hide_legend() {
    var legendDiv = document.getElementById("legend");  // Get the legend div
    var legendBtn = document.getElementById("legend_btn");  // Get the legend button

    if (legendDiv.style.display === "none" || legendDiv.style.display === "") {
        legendBtn.innerHTML = "Hide Legend";  // Update button text
        legendBtn.setAttribute("class", "btn btn-danger btn-sm");  // Change button style to red

        legendDiv.style.display = "block";  // Show legend
        legendDiv.style.width = "15%";
        legendDiv.style.height = "38%";
    } else {
        legendBtn.setAttribute("class", "btn btn-success btn-sm");  // Change button style to green
        legendBtn.innerHTML = "Show Legend";  // Update button text

        legendDiv.style.display = "none";  // Hide legend
        legendDiv.style.width = "0%";
        legendDiv.style.height = "0%";
    }
}


// ===================== FUNCTION TO CREATE LEGEND =====================
/**
 * Dynamically generates a legend based on the available WMS layers.
 */
function legend() {
    $('#legend').empty();  // Clear the existing legend content

    // Create legend title
    var head = document.createElement("h6");
    var txt = document.createTextNode("Legends");
    head.appendChild(txt);

    var element = document.getElementById("legend");
    element.appendChild(head);

    // Create a table for legend
    var table = document.createElement("table");
    table.style.width = "100%";
    table.style.borderCollapse = "collapse";
    table.style.border = "1px solid black"; // Ensure visible border

    // Create table header
    var thead = document.createElement("thead");
    var headerRow = document.createElement("tr");

//    var layerNameHeader = document.createElement("th");
//    layerNameHeader.appendChild(document.createTextNode("Layer Name"));
//    layerNameHeader.style.textAlign = "left";
//    layerNameHeader.style.padding = "8px";
//    layerNameHeader.style.border = "1px solid black";
//
//    var legendHeader = document.createElement("th");
//    legendHeader.appendChild(document.createTextNode("Color"));
//    legendHeader.style.textAlign = "center";
//    legendHeader.style.padding = "8px";
//    legendHeader.style.border = "1px solid black";

    //headerRow.appendChild(layerNameHeader);
    //headerRow.appendChild(legendHeader);
    thead.appendChild(headerRow);
    table.appendChild(thead);

    // Table body
    var tbody = document.createElement("tbody");

    // Debug: Check if overlays contain any layers
    var layersArray = overlays.getLayers().getArray();
    console.log("Number of layers in overlays:", layersArray.length);

    if (layersArray.length === 0) {
        console.warn("No overlay layers found. Check if overlays are being loaded correctly.");
    }

    // Iterate over each overlay layer and add rows to the table
    layersArray.forEach(layer => {
        var layerTitle = layer.get('title');

        // Debug: Check if layer title is valid
        if (!layerTitle) {
            console.warn("Layer found without a title:", layer);
            return; // Skip this iteration
        }

        console.log("Adding layer to legend:", layerTitle);

        var row = document.createElement("tr");

        // Layer Name Column
        var layerNameCell = document.createElement("td");
        layerNameCell.appendChild(document.createTextNode(layerTitle));
        layerNameCell.style.padding = "8px";
        layerNameCell.style.border = "1px solid black";

        // Legend Color Column
        var legendCell = document.createElement("td");
        legendCell.style.textAlign = "center";
        legendCell.style.padding = "8px";
        legendCell.style.border = "1px solid black";

        var img = new Image();
        img.src = "http://13.127.171.159:8080/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=20&LAYER=" + layerTitle;
        img.style.verticalAlign = "middle";
        img.onerror = function () {
            console.warn("Legend image not found for layer:", layerTitle);
            img.src = "fallback-legend.png"; // Fallback image if GeoServer legend is missing
        };

        legendCell.appendChild(img);
        row.appendChild(layerNameCell);
        row.appendChild(legendCell);
        tbody.appendChild(row);
    });

    table.appendChild(tbody);
    element.appendChild(table);
}


legend();  // Call the legend function to initialize the legend
