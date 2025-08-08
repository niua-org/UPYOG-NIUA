package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.MeasurementWithHeight;
import org.egov.common.entity.edcr.Room;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.service.LayerNames;
import org.egov.edcr.utility.Util;
import org.kabeja.dxf.DXFLWPolyline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VentilationExtract extends FeatureExtract {

	private static final Logger LOG = LogManager.getLogger(VentilationExtract.class);
	@Autowired
	private LayerNames layerNames;

	@Override
	public PlanDetail extract(PlanDetail pl) {
		for (Block b : pl.getBlocks()) {
			if (b.getBuilding() != null && b.getBuilding().getFloors() != null
					&& !b.getBuilding().getFloors().isEmpty()) {
				for (Floor f : b.getBuilding().getFloors()) {

					/*
					 * Adding general light and ventilation at floor level
					 */
					List<DXFLWPolyline> lightAndVentilations = Util.getPolyLinesByLayer(pl.getDoc(), String.format(
							layerNames.getLayerName("LAYER_NAME_LIGHT_VENTILATION"), b.getNumber(), f.getNumber()));
					if (!lightAndVentilations.isEmpty()) {
						List<Measurement> lightAndventilationMeasurements = lightAndVentilations.stream()
								.map(polyline -> new MeasurementDetail(polyline, true)).collect(Collectors.toList());
						f.getLightAndVentilation().setMeasurements(lightAndventilationMeasurements);

						f.getLightAndVentilation()
								.setHeightOrDepth((Util.getListOfDimensionValueByLayer(pl,
										String.format(layerNames.getLayerName("LAYER_NAME_LIGHT_VENTILATION"),
												b.getNumber(), f.getNumber()))));

					}
					/*
					 * Adding regular room wise light and ventilation
					 */
					for (Room room : f.getRegularRooms()) {
						String regularRoomLayerName = String.format(
								layerNames.getLayerName("LAYER_NAME_ROOM_LIGHT_VENTILATION"), b.getNumber(),
								f.getNumber(), room.getNumber(), "+\\d");

						List<String> regularRoomLayers = Util.getLayerNamesLike(pl.getDoc(), regularRoomLayerName);
						if (!regularRoomLayers.isEmpty()) {
							for (String regularRoomLayer : regularRoomLayers) {
								List<DXFLWPolyline> lightAndventilations = Util.getPolyLinesByLayer(pl.getDoc(),
										regularRoomLayer);
								if (!lightAndventilations.isEmpty()) {
									List<Measurement> lightAndventilationMeasurements = lightAndventilations.stream()
											.map(polyline -> new MeasurementDetail(polyline, true))
											.collect(Collectors.toList());
									room.getLightAndVentilation().setMeasurements(lightAndventilationMeasurements);

									room.getLightAndVentilation().setHeightOrDepth(
											(Util.getListOfDimensionValueByLayer(pl, regularRoomLayer)));
								}
							}
						}
					}
					/*
					 * Adding AC room wise light and ventilation
					 */
					for (Room room : f.getAcRooms()) {
						String acRoomLayerName = String.format(
								layerNames.getLayerName("LAYER_NAME_ACROOM_LIGHT_VENTILATION"), b.getNumber(),
								f.getNumber(), room.getNumber(), "+\\d");

						List<String> acRoomLayers = Util.getLayerNamesLike(pl.getDoc(), acRoomLayerName);
						if (!acRoomLayers.isEmpty()) {
							for (String acRoomLayer : acRoomLayers) {

								List<DXFLWPolyline> lightAndventilations = Util.getPolyLinesByLayer(pl.getDoc(),
										acRoomLayer);
								if (!lightAndventilations.isEmpty()) {
									List<Measurement> lightAndventilationMeasurements = lightAndventilations.stream()
											.map(polyline -> new MeasurementDetail(polyline, true))
											.collect(Collectors.toList());
									room.getLightAndVentilation().setMeasurements(lightAndventilationMeasurements);

									room.getLightAndVentilation()
											.setHeightOrDepth((Util.getListOfDimensionValueByLayer(pl, acRoomLayer)));

								}

							}
						}
					}
					
					// Kitchen and dining ventilation handling via new method
                    handleKitchenDiningVentilation(pl, b, f);

                    // Laundry and recreation ventilation handling via new method
                    handleLaundryRecreationVentilation(pl, b, f);
					

				}
			}
		}

		return pl;
	}
	
	private void handleKitchenDiningVentilation(PlanDetail pl, Block b, Floor f) {
        Room kitchen = f.getKitchen();
        if (kitchen != null) {
            String kitchenAndDining = String.format(
                    layerNames.getLayerName("LAYER_NAME_KITCHEN_DINING_VENTILATION"),
                    b.getNumber(), f.getNumber(), "+\\d");

            List<String> ventilationLayers = Util.getLayerNamesLike(pl.getDoc(), kitchenAndDining);
            if (!ventilationLayers.isEmpty()) {
                for (String ventLayer : ventilationLayers) {
                    List<DXFLWPolyline> lightAndVentilations = Util.getPolyLinesByLayer(pl.getDoc(), ventLayer);
                    if (!lightAndVentilations.isEmpty()) {
                        List<Measurement> lightAndVentilationMeasurements = lightAndVentilations.stream()
                            .map(polyline -> new MeasurementDetail(polyline, true))
                            .collect(Collectors.toList());
                        kitchen.getLightAndVentilation().setMeasurements(lightAndVentilationMeasurements);

                        kitchen.getLightAndVentilation().setHeightOrDepth(
                            Util.getListOfDimensionValueByLayer(pl, ventLayer));
                    }
                }
            }
        }
    }

    private void handleLaundryRecreationVentilation(PlanDetail pl, Block b, Floor f) {
        MeasurementWithHeight laundryVentilation = f.getLaundryOrRecreationalVentilation();
        if (laundryVentilation != null) {
            String laundryVentLayerPattern = String.format(
                    layerNames.getLayerName("LAYER_NAME_LAUNDRY_RECREATION_VENTILATION"),
                    b.getNumber(), f.getNumber(), "+\\d");

            List<String> ventilationLayers = Util.getLayerNamesLike(pl.getDoc(), laundryVentLayerPattern);
            if (!ventilationLayers.isEmpty()) {
                List<Measurement> ventilationMeasurements = new ArrayList<>();
                List<BigDecimal> heightsOrDepths = new ArrayList<>();

                for (String ventLayer : ventilationLayers) {
                    List<DXFLWPolyline> ventilations = Util.getPolyLinesByLayer(pl.getDoc(), ventLayer);
                    if (!ventilations.isEmpty()) {
                        List<Measurement> measurements = ventilations.stream()
                            .map(polyline -> new MeasurementDetail(polyline, true))
                            .collect(Collectors.toList());
                        ventilationMeasurements.addAll(measurements);

                        List<BigDecimal> dimensionValues = Util.getListOfDimensionValueByLayer(pl, ventLayer);
                        heightsOrDepths.addAll(dimensionValues);
                    }
                }

                laundryVentilation.setMeasurements(ventilationMeasurements);
                laundryVentilation.setHeightOrDepth(heightsOrDepths);
            }
        }
    }

	@Override
	public PlanDetail validate(PlanDetail pl) {
		return pl;
	}

}
