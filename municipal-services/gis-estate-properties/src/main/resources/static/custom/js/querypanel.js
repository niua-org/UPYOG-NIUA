// copied from old
var map, geojson, featureOverlay, overlays, style;
var selected, features, layer_name, layerControl;
var selectedFeature;


var source1 = new ol.source.Vector({
    wrapX: false
});

var vector1 = new ol.layer.Vector({
    source: source1
});
map.addLayer(vector1);

var draw1;


// clear function
function clear_all() {
    if (vector1) {
        vector1.getSource().clear();
        //map.removeLayer(geojson);
    }

    if (draw1) {
        map.removeInteraction(draw1);
    }
    document.getElementById('map').style.height = '100%';
    document.getElementById('table_data').style.height = '0%';
    map.updateSize();
    $('#table').empty();
    $('#legend').empty();
    if (geojson) {
        geojson.getSource().clear();
        map.removeLayer(geojson);
    }

    if (selectedFeature) {
        selectedFeature.setStyle();
        selectedFeature = undefined;
    }
    if (popup) {
        popup.hide();
    }
    map.getView().fit([65.90, 7.48, 98.96, 40.30], {
        duration: 1590,
        size: map.getSize()
    });


    document.getElementById("query_panel_btn").innerHTML = "☰ Open Query Panel";
    document.getElementById("query_panel_btn").setAttribute("class", "btn btn-success btn-sm");

    document.getElementById("query_tab").style.width = "0%";
    document.getElementById("map").style.width = "100%";
    document.getElementById("map").style.left = "0%";
    document.getElementById("query_tab").style.visibility = "hidden";
    document.getElementById('table_data').style.left = '0%';

    document.getElementById("legend_btn").innerHTML = "☰ Show Legend";
    document.getElementById("legend").style.width = "0%";
    document.getElementById("legend").style.visibility = "hidden";
    document.getElementById('legend').style.height = '0%';

    map.un('singleclick', getinfo);
    map.un('singleclick', highlight);
    document.getElementById("info_btn").innerHTML = "☰ Activate GetInfo";
    document.getElementById("info_btn").setAttribute("class", "btn btn-success btn-sm");
    map.updateSize();

    overlays.getLayers().getArray().slice().forEach(layer => {
        overlays.getLayers().remove(layer);
    });

    layerSwitcher.renderPanel();

    if (draw) {
        map.removeInteraction(draw)
    };
    if (vectorLayer) {
        vectorLayer.getSource().clear();
    }
    map.removeOverlay(helpTooltip);

    if (measureTooltipElement) {
        var elem = document.getElementsByClassName("tooltip tooltip-static");
        //$('#measure_tool').empty();

        //alert(elem.length);
        for (var i = elem.length - 1; i >= 0; i--) {

            elem[i].remove();
            //alert(elem[i].innerHTML);
        }
    }
}

// layer dropdown query
$(document).ready(function() {
    $.ajax({
        type: "GET",
        url: "http://13.127.171.159:8080/geoserver/wfs?request=getCapabilities",
        dataType: "xml",
        success: function(xml) {
            var select = $('#layer');
            $(xml).find('FeatureType').each(function() {
                //var title = $(this).find('ows:Operation').attr('name');
                //alert(title);
                var name = $(this).find('Name').text();
                //select.append("<option/><option class='ddheader' value='"+ name +"'>"+title+"</option>");
                $(this).find('Name').each(function() {
                    var value = $(this).text();
                    select.append("<option class='ddindent' value='" + value + "'>" + value + "</option>");
                });
            });
            //select.children(":first").text("please make a selection").attr("selected",true);
        }
    });
});

// attribute dropdown
$(function() {
    $("#layer").change(function() {

        var attributes = document.getElementById("attributes");
        var length = attributes.options.length;
        for (i = length - 1; i >= 0; i--) {
            attributes.options[i] = null;
        }

        var value_layer = $(this).val();


        attributes.options[0] = new Option('Select attributes', "");
        //  alert(url);

        $(document).ready(function() {
            $.ajax({
                type: "GET",
                url: "http://13.127.171.159:8080/geoserver/wfs?service=WFS&request=DescribeFeatureType&version=1.1.0&typeName=" + value_layer,
                dataType: "xml",
                success: function(xml) {

                    var select = $('#attributes');
                    //var title = $(xml).find('xsd\\:complexType').attr('name');
                    //	alert(title);
                    $(xml).find('xsd\\:sequence').each(function() {

                        $(this).find('xsd\\:element').each(function() {
                            var value = $(this).attr('name');
                            //alert(value);
                            var type = $(this).attr('type');
                            //alert(type);
                            if (value != 'geom' && value != 'the_geom') {
                                select.append("<option class='ddindent' value='" + type + "'>" + value + "</option>");
                            }
                        });

                    });
                }
            });
        });


    });
});

// operator combo
$(function() {
    $("#attributes").change(function() {

        var operator = document.getElementById("operator");
        var length = operator.options.length;
        for (i = length - 1; i >= 0; i--) {
            operator.options[i] = null;
        }

        var value_type = $(this).val();
        // alert(value_type);
        var value_attribute = $('#attributes option:selected').text();
        operator.options[0] = new Option('Select operator', "");

        if (value_type == 'xsd:short' || value_type == 'xsd:int' || value_type == 'xsd:double' || value_type == 'xsd:long') {
            var operator1 = document.getElementById("operator");
            operator1.options[1] = new Option('Greater than', '>');
            operator1.options[2] = new Option('Less than', '<');
            operator1.options[3] = new Option('Equal to', '=');
            operator1.options[4] = new Option('Between', 'BETWEEN');
        } else if (value_type == 'xsd:string') {
            var operator1 = document.getElementById("operator");
            operator1.options[1] = new Option('Like', 'ILike');

        }

    });
});



// layer dropdown draw query
$(document).ready(function() {
    $.ajax({
        type: "GET",
        url: "http://13.127.171.159:8080/geoserver/wfs?request=getCapabilities",
        dataType: "xml",
        success: function(xml) {
            var select = $('#layer1');
            $(xml).find('FeatureType').each(function() {
                //var title = $(this).find('ows:Operation').attr('name');
                //alert(title);
                var name = $(this).find('Name').text();
                //select.append("<option/><option class='ddheader' value='"+ name +"'>"+title+"</option>");
                $(this).find('Name').each(function() {
                    var value = $(this).text();
                    select.append("<option class='ddindent' value='" + value + "'>" + value + "</option>");
                });
            });
            //select.children(":first").text("please make a selection").attr("selected",true);
        }
    });
});


var highlightStyle = new ol.style.Style({
    fill: new ol.style.Fill({
        color: 'rgba(255,0,0,0.3)',
    }),
    stroke: new ol.style.Stroke({
        color: '#3399CC',
        width: 3,
    }),
    image: new ol.style.Circle({
        radius: 10,
        fill: new ol.style.Fill({
            color: '#3399CC'
        })
    })
});

// function for finding row in the table when feature selected on map
function findRowNumber(cn1, v1) {

    var table = document.querySelector('#table');
    var rows = table.querySelectorAll("tr");
    var msg = "No such row exist"
    for (i = 1; i < rows.length; i++) {
        var tableData = rows[i].querySelectorAll("td");
        if (tableData[cn1 - 1].textContent == v1) {
            msg = i;
            break;
        }
    }
    return msg;
}

// function for loading query

function query() {

    $('#table').empty();
    if (geojson) {
        map.removeLayer(geojson);

    }
    if (selectedFeature) {
        selectedFeature.setStyle();
        selectedFeature = undefined;
    }
	if (vector1) {
        vector1.getSource().clear();
		// $('#table').empty();
    }

    //alert('jsbchdb');
    var layer = document.getElementById("layer");
    var value_layer = layer.options[layer.selectedIndex].value;
    //alert(value_layer);

    var attribute = document.getElementById("attributes");
    var value_attribute = attribute.options[attribute.selectedIndex].text;
    //alert(value_attribute);

    var operator = document.getElementById("operator");
    var value_operator = operator.options[operator.selectedIndex].value;
    //alert(value_operator);

    var txt = document.getElementById("value");
    var value_txt = txt.value;

    if (value_operator == 'ILike') {
        value_txt = "'" + value_txt + "%25'";
        //alert(value_txt);
        //value_attribute = 'strToLowerCase('+value_attribute+')';
    } else {
        value_txt = value_txt;
        //value_attribute = value_attribute;
    }
    //alert(value_txt);




    var url = "http://13.127.171.159:8080/geoserver/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=" + value_layer + "&CQL_FILTER=" + value_attribute + "%20" + value_operator + "%20" + value_txt + "&outputFormat=application/json"
    //console.log(url);

    style = new ol.style.Style({
        fill: new ol.style.Fill({
            color: 'rgba(255, 255, 255, 0.2)'
        }),
        stroke: new ol.style.Stroke({
            color: '#ffcc33',
            width: 3
        }),

        image: new ol.style.Circle({
            radius: 7,
            fill: new ol.style.Fill({
                color: '#ffcc33'
            })
        })
    });
    geojson = new ol.layer.Vector({
        //title:'dfdfd',
        //title: '<h5>' + value_crop+' '+ value_param +' '+ value_seas+' '+value_level+'</h5>',
        source: new ol.source.Vector({
            url: url,
            format: new ol.format.GeoJSON()
        }),
        style: style,

    });

    geojson.getSource().on('addfeature', function() {
        //alert(geojson.getSource().getExtent());
        map.getView().fit(
            geojson.getSource().getExtent(), {
                duration: 1590,
                size: map.getSize()
            }
        );
    });

    //overlays.getLayers().push(geojson);
    map.addLayer(geojson);

    $.getJSON(url, function(data) {


        var col = [];
        col.push('id');
        for (var i = 0; i < data.features.length; i++) {

            for (var key in data.features[i].properties) {

                if (col.indexOf(key) === -1) {
                    col.push(key);
                }
            }
        }



        var table = document.createElement("table");
        table.setAttribute("class", "table table-hover table-striped");
        table.setAttribute("id", "table");

        var caption = document.createElement("caption");
        caption.setAttribute("id", "caption");
        caption.style.captionSide = 'top';
        caption.innerHTML = value_layer + " (Number of Features : " + data.features.length + " )";
        table.appendChild(caption);



        // CREATE HTML TABLE HEADER ROW USING THE EXTRACTED HEADERS ABOVE.

        var tr = table.insertRow(-1); // TABLE ROW.

        for (var i = 0; i < col.length; i++) {
            var th = document.createElement("th"); // TABLE HEADER.
            th.innerHTML = col[i];
            tr.appendChild(th);
        }

        // ADD JSON DATA TO THE TABLE AS ROWS.
        for (var i = 0; i < data.features.length; i++) {

            tr = table.insertRow(-1);

            for (var j = 0; j < col.length; j++) {
                var tabCell = tr.insertCell(-1);
                if (j == 0) {
                    tabCell.innerHTML = data.features[i]['id'];
                } else {
                    //alert(data.features[i]['id']);
                    tabCell.innerHTML = data.features[i].properties[col[j]];
                    //alert(tabCell.innerHTML);
                }
            }
        }


        // FINALLY ADD THE NEWLY CREATED TABLE WITH JSON DATA TO A CONTAINER.
        var divContainer = document.getElementById("table_data");
        divContainer.innerHTML = "";
        divContainer.appendChild(table);



        document.getElementById('map').style.height = '71%';
        document.getElementById('table_data').style.height = '29%';
        map.updateSize();
        addRowHandlers();

    });
    map.on('singleclick', highlight);



}



// highlight the feature on map and table on map click
function highlight(evt) {

    if (selectedFeature) {
        selectedFeature.setStyle();
        selectedFeature = undefined;
    }

    var feature = map.forEachFeatureAtPixel(evt.pixel,
        function(feature, layer) {
            return feature;
        });

    if (feature && feature.getId() != undefined) {


        var geometry = feature.getGeometry();
        var coord = geometry.getCoordinates();
        var coordinate = evt.coordinate;
        //alert(feature.get('gid'));
        // alert(coordinate);
        /*var content1 = '<h3>' + feature.get([name]) + '</h3>';
        content1 += '<h5>' + feature.get('crop')+' '+ value_param +' '+ value_seas+' '+value_level+'</h5>'
		content1 += '<h5>' + feature.get([value_param]) +' '+ unit +'</h5>';

       // alert(content1);
        content.innerHTML = content1;
        overlay.setPosition(coordinate);*/

        // console.info(feature.getProperties());

        $(function() {
            $("#table td").each(function() {
                $(this).parent("tr").css("background-color", "white");
            });
        });
        feature.setStyle(highlightStyle);
        selectedFeature = feature;
        var table = document.getElementById('table');
        var cells = table.getElementsByTagName('td');
        var rows = document.getElementById("table").rows;
        var heads = table.getElementsByTagName('th');
        var col_no;
        for (var i = 0; i < heads.length; i++) {
            // Take each cell
            var head = heads[i];
            //alert(head.innerHTML);
            if (head.innerHTML == 'id') {
                col_no = i + 1;
                //alert(col_no);
            }

        }
        var row_no = findRowNumber(col_no, feature.getId());
        //alert(row_no);

        var rows = document.querySelectorAll('#table tr');

        rows[row_no].scrollIntoView({
            behavior: 'smooth',
            block: 'center'
        });

        $(document).ready(function() {
            $("#table td:nth-child(" + col_no + ")").each(function() {

                if ($(this).text() == feature.getId()) {
                    $(this).parent("tr").css("background-color", "lightgrey");

                }
            });
        });
    } else {
        $(function() {
            $("#table td").each(function() {
                $(this).parent("tr").css("background-color", "white");
            });
        });

    }




    /*$(function() {
  $("#table td").each(function() {
    if ($(this).text() == feature.get('gid')) {
     // $(this).css('color', 'red');
	   $(this).parent("tr").css("background-color", "grey");
    }
  });
});*/




};

// highlight the feature on map and table on row select in table
function addRowHandlers() {
    var rows = document.getElementById("table").rows;
    var heads = table.getElementsByTagName('th');
    var col_no;
    for (var i = 0; i < heads.length; i++) {
        // Take each cell
        var head = heads[i];
        //alert(head.innerHTML);
        if (head.innerHTML == 'id') {
            col_no = i + 1;
            //alert(col_no);
        }

    }
    for (i = 0; i < rows.length; i++) {

        rows[i].onclick = function() {
            return function() {
                if (selectedFeature) {
                    selectedFeature.setStyle();
                    selectedFeature = undefined;
                }
                $(function() {
                    $("#table td").each(function() {
                        $(this).parent("tr").css("background-color", "white");
                    });
                });
                var cell = this.cells[col_no - 1];
                var id = cell.innerHTML;


                $(document).ready(function() {
                    $("#table td:nth-child(" + col_no + ")").each(function() {
                        if ($(this).text() == id) {
                            $(this).parent("tr").css("background-color", "lightgrey");
                        }
                    });
                });

                var features = geojson.getSource().getFeatures();

                for (i = 0; i < features.length; i++) {
                    if (features[i].getId() == id) {
                        //alert(features[i].feature.id);
                        features[i].setStyle(highlightStyle);
                        selectedFeature = features[i];
                        var featureExtent = features[i].getGeometry().getExtent();
                        if (featureExtent) {
                            map.getView().fit(featureExtent, {
                                duration: 1590,
                                size: map.getSize()
                            });
                        }

                    }
                }

                //alert("id:" + id);
            };
        }(rows[i]);
    }
}

//function show_hide_querypanel() {
//
//    if (document.getElementById("query_tab").style.visibility == "hidden") {
//        document.getElementById("query_panel_btn").innerHTML = "☰ Hide Query Panel";
//        document.getElementById("query_panel_btn").setAttribute("class", "btn btn-danger btn-sm");
//        document.getElementById("query_tab").style.visibility = "visible";
//        document.getElementById("query_tab").style.width = "21%";
//        document.getElementById("map").style.width = "79%";
//        document.getElementById("map").style.left = "21%";
//
//        document.getElementById('table_data').style.left = '21%';
//        map.updateSize();
//    } else {
//        document.getElementById("query_panel_btn").innerHTML = "Open Query Panel";
//        document.getElementById("query_panel_btn").setAttribute("class", "btn btn-success btn-sm");
//        document.getElementById("query_tab").style.width = "0%";
//        document.getElementById("map").style.width = "100%";
//        document.getElementById("map").style.left = "0%";
//        document.getElementById("query_tab").style.visibility = "hidden";
//        document.getElementById('table_data').style.left = '0%';
//
//        map.updateSize();
//    }
//}

function show_hide_querypanel() {
    var queryTab = document.getElementById("query_tab");
    var queryPanelBtn = document.getElementById("query_panel_btn");

    // ✅ Handle empty visibility state on the first click
    if (queryTab.style.visibility === "hidden" || queryTab.style.visibility === "") {
        queryPanelBtn.innerHTML = "Hide Query Panel";
        queryPanelBtn.className = "btn btn-danger btn-sm";
        queryTab.style.visibility = "visible";
        queryTab.style.width = "21%";
        document.getElementById("map").style.width = "79%";
        document.getElementById("map").style.left = "21%";
        document.getElementById('table_data').style.left = '21%';
    } else {
        queryPanelBtn.innerHTML = "Open Query Panel";
        queryPanelBtn.className = "btn btn-success btn-sm";
        queryTab.style.width = "0%";
        document.getElementById("map").style.width = "100%";
        document.getElementById("map").style.left = "0%";
        queryTab.style.visibility = "hidden";  // ✅ This now works
        document.getElementById('table_data').style.left = '0%';
    }

    map.updateSize();
}


