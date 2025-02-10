//\
//// All Global Variable
//var draw
//var flagIsDrawingOn = false
//var PointType = ['ATM','Tree','Telephone Poles', 'Electricity Poles'];
//var LineType = ['National Highway','State Highway','River','Telephone Lines'];
//var PolygonType = ['Water Body','Commercial Land', 'Residential Land','Building'];
//var selectedGeomType
//
//
//// Custom popup
//// Popup overlay with popupClass=anim
//var popup = new ol.Overlay.Popup ({
//    popupClass: "default anim", //"tooltips", "warning" "black" "default", "tips", "shadow",
//    closeBox: true,
//    onclose: function(){ console.log("You close the box"); },
//    positioning: 'auto',
//    autoPan: true,
//    autoPanAnimation: { duration: 100 }
//  });
//// Custom Control
// /**
//       * Define a namespace for the application.
//       */
//      window.app = {};
//      var app = window.app;
//
//
//      //
//      // Define rotate to north control.
//      //
//
//
//      /**
//       * @constructor
//       * @extends {ol.control.Control}
//       * @param {Object=} opt_options Control options.
//       */
//      app.DrawingApp = function(opt_options) {
//
//        var options = opt_options || {};
//
//        var button = document.createElement('button');
//        button.id = 'drawbtn'
//        button.innerHTML = '<i class="fas fa-pencil-ruler"></i>';
//
//        var this_ = this;
//        var startStopApp = function() {
//            if (flagIsDrawingOn == false){
//       $('#startdrawModal').modal('show')
//
//            } else {
//                map.removeInteraction(draw)
//                flagIsDrawingOn = false
//                document.getElementById('drawbtn').innerHTML = '<i class="fas fa-pencil-ruler"></i>'
//                defineTypeofFeature()
//                $("#enterInformationModal").modal('show')
//
//            }
//        };
//
//        button.addEventListener('click', startStopApp, false);
//        button.addEventListener('touchstart', startStopApp, false);
//
//        var element = document.createElement('div');
//        element.className = 'draw-app ol-unselectable ol-control';
//        element.appendChild(button);
//
//        ol.control.Control.call(this, {
//          element: element,
//          target: options.target
//        });
//
//      };
//      ol.inherits(app.DrawingApp, ol.control.Control);
//
//
//      //
//      // Create map, giving it a rotate to north control.
//      //
//
//
//
//// View
//var myview = new ol.View({
//    center : [8214563.509192685, 2272903.8536058646],
//    zoom:14
//})
//
//// OSM Layer
//var baseLayer = new ol.layer.Tile({
//    title: 'OSM Base Map',
//    type: 'base',
//    source : new ol.source.OSM({
//        attributions:'L&E GIS'
//    })
//})
//
//// Geoserver Layer
//var featureLayersource = new ol.source.TileWMS({
//    url:'http://13.127.171.159:8080/geoserver/wms',
//    params:{'LAYERS':'LNEstates_DB:Estates', 'tiled' : true},
//    serverType:'geoserver'
//})
//
//var featureLayer = new ol.layer.Tile({
//    title: 'Feature Map Layer',
//    type: 'base',
//    source:featureLayersource
//})
//// Draw vector layer
//// 1 . Define source
//var drawSource = new ol.source.Vector()
//// 2. Define layer
//var drawLayer = new ol.layer.Vector({
//    title: 'Drawn Map Base',
//    type: 'base',
//    source : drawSource
//})
//// Layer Array
//var layerArray = [baseLayer,featureLayer, drawLayer]
//// Map
//var map = new ol.Map({
//    controls: ol.control.defaults({
//        attributionOptions: {
//          collapsible: false
//        }
//      }).extend([
//        new app.DrawingApp()
//      ]),
//    target : 'mymap',
//    view: myview,
//    layers:layerArray,
//    overlays: [popup]
//})
//
//console.log("All Layers in Map:", map.getLayers().getArray());
//
//
//
//
//
//
//// Function to start Drawing
//function startDraw(geomType){
//    selectedGeomType = geomType
//    draw = new ol.interaction.Draw({
//        type:geomType,
//        source:drawSource
//    })
//    $('#startdrawModal').modal('hide')
//
//    map.addInteraction(draw)
//    flagIsDrawingOn = true
//    document.getElementById('drawbtn').innerHTML = '<i class="far fa-stop-circle"></i>'
//}
//
//
//// Function to add types based on feature
//function defineTypeofFeature(){
//    var dropdownoftype = document.getElementById('typeofFeatures')
//    dropdownoftype.innerHTML = ''
//    if (selectedGeomType == 'Point'){
//        for (i=0;i<PointType.length;i++){
//            var op = document.createElement('option')
//            op.value = PointType[i]
//            op.innerHTML = PointType[i]
//            dropdownoftype.appendChild(op)
//        }
//    } else if (selectedGeomType == 'LineString'){
//        for (i=0;i<LineType.length;i++){
//            var op = document.createElement('option')
//            op.value = LineType[i]
//            op.innerHTML = LineType[i]
//            dropdownoftype.appendChild(op)
//        }
//    }else{
//        for (i=0;i<PolygonType.length;i++){
//            var op = document.createElement('option')
//            op.value = PolygonType[i]
//            op.innerHTML = PolygonType[i]
//            dropdownoftype.appendChild(op)
//        }
//    }
//}
//
//
//// Function to save information in db
//function savetodb(){
//    // get array of all features
//    var featureArray = drawSource.getFeatures()
//    // Define geojson format
//    var geogJONSformat = new ol.format.GeoJSON()
//    // Use method to convert feature to geojson
//    var featuresGeojson = geogJONSformat.writeFeaturesObject(featureArray)
//    // Array of all geojson
//    var geojsonFeatureArray = featuresGeojson.features
//
//    for (i=0;i<geojsonFeatureArray.length;i++){
//        var type = document.getElementById('typeofFeatures').value
//        var name = document.getElementById('exampleInputtext1').value
//        var geom = JSON.stringify(geojsonFeatureArray[i].geometry)
//        console.log("📌 Debugging GeoJSON Data before saving:");
//        console.log("🔹 Geometry Type:", type);
//        console.log("🔹 Feature Name:", name);
//        console.log("🔹 Geometry (GeoJSON):", geom);
//        if (type != ''){
//            $.ajax({
//                url:'save.php',
//                type:'POST',
//                data :{
//                    typeofgeom : type,
//                    nameofgeom : name,
//                    stringofgeom : geom
//                },
//                success : function(dataResult){
//                    var result = JSON.parse(dataResult)
//                    if (result.statusCode == 200){
//                        console.log('data added successfully')
//                    } else {
//                        console.log('data not added successfully')
//                    }
//
//                }
//            })
//        } else {
//            alert('please select type')
//        }
//    }
//
//    // Update layer
//    var params = featureLayer.getSource().getParams();
//    params.t = new Date().getMilliseconds();
//    featureLayer.getSource().updateParams(params);
//
//    // Close the Modal
//    $("#enterInformationModal").modal('hide')
//
//    clearDrawSource ()
//
//}
//
//
//function clearDrawSource (){
//    drawSource.clear()
//}
//
//
//// Geolocation
//  // set up geolocation to track our position
//  var geolocation = new ol.Geolocation({
//    tracking: true,
//    projection : map.getView().getProjection(),
//    enableHighAccuracy: true,
//  });
//  // bind it to the view's projection and update the view as we move
////   geolocation.bindTo('projection', myview);
//  geolocation.on('change:position', function() {
//    myview.setCenter(geolocation.getPosition());
//    addmarker(geolocation.getPosition())
//  });
////   // add a marker to display the current location
//  var marker = new ol.Overlay({
//    element: document.getElementById('currentLocation'),
//    positioning: 'center-center',
//    // position:  geolocation
//  });
//  map.addOverlay(marker);
//  // and bind it to the geolocation's position updates
//
//  function addmarker(array){
//  marker.setPosition(array);
////   myview.setZoom(16)
//   }
//
//  // create a new device orientation object set to track the device
////   var deviceOrientation = new ol.DeviceOrientation({
////     tracking: true
////   });
////   // when the device changes heading, rotate the view so that
////   // 'up' on the device points the direction we are facing
////   deviceOrientation.on('change:heading', onChangeHeading);
////   function onChangeHeading(event) {
////     var heading = event.target.getHeading();
////     view.setRotation(-heading);
////   }
//
//if (window.DeviceOrientationEvent) {
//    window.addEventListener("deviceorientation", function(event) {
//        if (event.alpha !== null) {
//            var heading = event.alpha * (Math.PI / 180); // Convert degrees to radians
//            map.getView().setRotation(-heading);
//            console.log("Device heading:", event.alpha);
//        }
//    });
//} else {
//    console.log("DeviceOrientation API not supported in this browser.");
//}
//
//// var base_maps = new ol.layer.Group({
////     'title': 'Base maps',
////     layers: [
////         new ol.layer.Tile({
////             title: 'Satellite',
////             type: 'base',
////             visible: true,
////             source: new ol.source.XYZ({
////                 attributions: ['Powered by Esri',
////                     'Source: Esri, DigitalGlobe, GeoEye, Earthstar Geographics, CNES/Airbus DS, USDA, USGS, AeroGRID, IGN, and the GIS User Community'
////                 ],
////                 attributionsCollapsible: false,
////                 url: 'https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
////                 maxZoom: 23
////             })
////         }),
////         new ol.layer.Tile({
////             title: 'OSM',
////             type: 'base',
////             visible: true,
////             source: new ol.source.OSM()
////         })
//
//
////     ]
//// });
//
//var OSM = new ol.layer.Tile({
//    source: new ol.source.OSM(),
//    type: 'base',
//    title: 'OSM',
//});
//
//overlays = new ol.layer.Group({
//    'title': 'Overlays',
//    layers: []
//});
//
//layerSwitcher = new ol.control.LayerSwitcher({
//    activationMode: 'click',
//    startActive: true,
//    tipLabel: 'Layers', // Optional label for button
//    groupSelectStyle: 'children', // Can be 'children' [default], 'group' or 'none'
//    collapseTipLabel: 'Collapse layers',
//});
////map.addControl(layerSwitcher);
//map.addControl(layerSwitcher);
//
//
//// function show_hide_legend() {
//
////     if (document.getElementById("legend").style.visibility == "hidden") {
//
////         document.getElementById("legend_btn").innerHTML = "☰ Hide Legend";
//// 		document.getElementById("legend_btn").setAttribute("class", "btn btn-danger btn-sm");
//
////         document.getElementById("legend").style.visibility = "visible";
////         document.getElementById("legend").style.width = "15%";
//
////         document.getElementById('legend').style.height = '38%';
////         map.updateSize();
////     } else {
//// 	    document.getElementById("legend_btn").setAttribute("class", "btn btn-success btn-sm");
////         document.getElementById("legend_btn").innerHTML = "☰ Show Legend";
////         document.getElementById("legend").style.width = "0%";
////         document.getElementById("legend").style.visibility = "hidden";
////         document.getElementById('legend').style.height = '0%';
//
////         map.updateSize();
////     }
//// }
//
//
//function show_hide_legend() {
//    var legendDiv = document.getElementById("legend");
//    var legendBtn = document.getElementById("legend_btn");
//
//    if (legendDiv.style.display === "none" || legendDiv.style.display === "") {
//        legendBtn.innerHTML = "☰ Hide Legend";
//        legendBtn.setAttribute("class", "btn btn-danger btn-sm");
//
//        legendDiv.style.display = "block"; // Show legend
//        legendDiv.style.width = "15%";
//        legendDiv.style.height = "38%";
//    } else {
//        legendBtn.setAttribute("class", "btn btn-success btn-sm");
//        legendBtn.innerHTML = "☰ Show Legend";
//
//        legendDiv.style.display = "none"; // Hide legend
//        legendDiv.style.width = "0%";
//        legendDiv.style.height = "0%";
//    }
//}
//
//
////legend
//function legend() {
//
//    $('#legend').empty();
//    var no_layers = overlays.getLayers().get('length');
//    //console.log(no_layers[0].options.layers);
//    // console.log(overlays.getLayers().get('length'));
//    //var no_layers = overlays.getLayers().get('length');
//
//    var head = document.createElement("h8");
//
//    var txt = document.createTextNode("Legend");
//
//    head.appendChild(txt);
//    var element = document.getElementById("legend");
//    element.appendChild(head);
//
//
//    overlays.getLayers().getArray().slice().forEach(layer => {
//
//        var head = document.createElement("p");
//
//        var txt = document.createTextNode(layer.get('title'));
//        //alert(txt[i]);
//        head.appendChild(txt);
//        var element = document.getElementById("legend");
//        element.appendChild(head);
//        var img = new Image();
//        img.src = "http://13.127.171.159:8080/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=20&LAYER=" + layer.get('title');
//        var src = document.getElementById("legend");
//        src.appendChild(img);
//
//    });
//
//}
//
//legend();
////layerSwitcher.renderPanel();
//
//// Get information about feature
//map.on('click', function(evt){
//    popup.hide();
//    var resolution  = map.getView().getResolution();
//    var coord = evt.coordinate
//    var projection = map.getView().getProjection()
//
//    console.log(featureLayersource instanceof ol.source.TileWMS);
//    console.log("Feature Source Object:", featureLayersource);
//    var url = featureLayersource.getFeatureInfoUrl(coord,resolution,projection,{'INFO_FORMAT':'application/json'})
//    console.log(url)
//    if (url){
//        $.getJSON(url,function(data){
//            console.log(data)
//            content = '<b>TYPE</b> : '+data.features[0].properties.type +' <br> <b>NAME </b> : '+data.features[0].properties.name
//            if (data.features[0].geometry.type == 'Polygon'){
//                popup.show(data.features[0].geometry.coordinates[0][0], content);
//            } else if (data.features[0].geometry.type == 'Point'){
//                popup.show(data.features[0].geometry.coordinates, content);
//            } else {
//                popup.show( data.features[0].geometry.coordinates[0], content);
//            }
//
//        })
//    }
//})
//
//
////list of wms_layers_ in window on click of button
//
//function wms_layers() {
//
//    $(function() {
//
//        $("#wms_layers_window").modal({
//            backdrop: false
//        });
//        $("#wms_layers_window").draggable();
//        $("#wms_layers_window").modal('show');
//
//    });
//
//    $(document).ready(function() {
//        $.ajax({
//            type: "GET",
//            url: "http://13.127.171.159:8080/geoserver/wms?request=getCapabilities",
//            dataType: "xml",
//            success: function(xml) {
//                $('#table_wms_layers').empty();
//                // console.log("here");
//                $('<tr></tr>').html('<th>Name</th><th>Title</th><th>Abstract</th>').appendTo('#table_wms_layers');
//                $(xml).find('Layer').find('Layer').each(function() {
//                    var name = $(this).children('Name').text();
//                    // alert(name);
//                    //var name1 = name.find('Name').text();
//                    //alert(name);
//                    var title = $(this).children('Title').text();
//
//                    var abst = $(this).children('Abstract').text();
//                    //   alert(abst);
//
//
//                    //   alert('test');
//                    $('<tr></tr>').html('<td>' + name + '</td><td>' + title + '</td><td>' + abst + '</td>').appendTo('#table_wms_layers');
//                    //document.getElementById("table_wms_layers").setAttribute("class", "table-success");
//
//                });
//                addRowHandlers1();
//            }
//        });
//    });
//
//
//
//
//    function addRowHandlers1() {
//        //alert('knd');
//        var rows = document.getElementById("table_wms_layers").rows;
//        var table = document.getElementById('table_wms_layers');
//        var heads = table.getElementsByTagName('th');
//        var col_no;
//        for (var i = 0; i < heads.length; i++) {
//            // Take each cell
//            var head = heads[i];
//            //alert(head.innerHTML);
//            if (head.innerHTML == 'Name') {
//                col_no = i + 1;
//                //alert(col_no);
//            }
//
//        }
//        for (i = 0; i < rows.length; i++) {
//
//            rows[i].onclick = function() {
//                return function() {
//
//                    $(function() {
//                        $("#table_wms_layers td").each(function() {
//                            $(this).parent("tr").css("background-color", "white");
//                        });
//                    });
//                    var cell = this.cells[col_no - 1];
//                    layer_name = cell.innerHTML;
//                    // alert(layer_name);
//
//                    $(document).ready(function() {
//                        $("#table_wms_layers td:nth-child(" + col_no + ")").each(function() {
//                            if ($(this).text() == layer_name) {
//                                $(this).parent("tr").css("background-color", "grey");
//
//
//
//                            }
//                        });
//                    });
//
//                    //alert("id:" + id);
//                };
//            }(rows[i]);
//        }
//
//    }
//
//}
//
//// // add wms layer to map on click of button
//// function add_layer() {
////     //	alert("jd");
//
////     // alert(layer_name);
////     //map.removeControl(layerSwitcher);
//
////     var name = layer_name.split(":");
////     //alert(layer_name);
////     var layer_wms = new ol.layer.Image({
////         title: layer_name,
////         // extent: [-180, -90, -180, 90],
////         source: new ol.source.ImageWMS({
////             url: 'http://13.127.171.159:8080/geoserver/wms',
////             params: {
////                 'LAYERS': layer_name
////             },
////             ratio: 1,
////             serverType: 'geoserver'
////         })
////     });
////     overlays.getLayers().push(layer_wms);
//
////     var url = 'http://13.127.171.159:8080/geoserver/wms?request=getCapabilities';
////     var parser = new ol.format.WMSCapabilities();
//
//
////     $.ajax(url).then(function(response) {
////         //window.alert("word");
////         var result = parser.read(response);
////         // console.log(result);
////         // window.alert(result);
////         var Layers = result.Capability.Layer.Layer;
////         var extent;
////         for (var i = 0, len = Layers.length; i < len; i++) {
//
////             var layerobj = Layers[i];
////             //  window.alert(layerobj.Name);
//
////             if (layerobj.Name == layer_name) {
////                 extent = layerobj.BoundingBox[0].extent;
////                 //alert(extent);
////                 map.getView().fit(
////                     extent, {
////                         duration: 1590,
////                         size: map.getSize()
////                     }
////                 );
//
////             }
////         }
////     });
//
//
////     //layerSwitcher.renderPanel();
////     map.addControl(layerSwitcher);
////     legend();
//
//// }
//
//function add_layer() {
//    if (!layer_name) {
//        alert("No WMS layer selected.");
//        return;
//    }
//
//    var name = layer_name.split(":")[1] || layer_name; // Extract actual layer name if namespace exists
//
//    // Check if the layer is already added
//    var existingLayers = overlays.getLayers().getArray();
//    for (var i = 0; i < existingLayers.length; i++) {
//        if (existingLayers[i].get('title') === name) {
//            alert("Layer already added to the map.");
//            return;
//        }
//    }
//
//    var layer_wms = new ol.layer.Image({
//        title: name,
//        source: new ol.source.ImageWMS({
//            url: 'http://13.127.171.159:8080/geoserver/wms',
//            params: { 'LAYERS': layer_name, 'TILED': true },
//            ratio: 1,
//            serverType: 'geoserver'
//        })
//    });
//
//    overlays.getLayers().push(layer_wms);
//    map.addLayer(layer_wms);
//
//    // Ensure LayerSwitcher updates
//    layerSwitcher.renderPanel();
//
//
//
//    // Auto-zoom to new layer's extent
//    var url = 'http://13.127.171.159:8080/geoserver/wms?request=getCapabilities';
//    var parser = new ol.format.WMSCapabilities();
//
//    $.ajax(url).then(function(response) {
//        var result = parser.read(response);
//        var Layers = result.Capability.Layer.Layer;
//
//        for (var i = 0; i < Layers.length; i++) {
//            if (Layers[i].Name === layer_name) {
//                var extent = Layers[i].BoundingBox[0].extent;
//                if (extent) {
//                    map.getView().fit(extent, { duration: 1200 });
//                }
//                break;
//            }
//        }
//    });
//
//    // Refresh legend after adding a new layer
//    legend();
//
//    console.log("Layer added:", name);
//}
//
//
//function close_wms_window() {
//    layer_name = undefined;
//}