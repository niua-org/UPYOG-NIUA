package org.egov.edcr.feature;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.egov.common.entity.edcr.*;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.service.LayerNames;
import org.egov.edcr.service.ConfigCacheService;
import org.egov.edcr.utility.Util;
import org.kabeja.dxf.DXFLWPolyline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerandahExtract extends FeatureExtract {

	private static final Logger LOG = LogManager.getLogger(VerandahExtract.class);
	@Autowired
	private LayerNames layerNames;

	@Autowired
	private ConfigCacheService configCacheService;

	@Override
	public PlanDetail validate(PlanDetail pl) {
		return pl;
	}

	@Override
	public PlanDetail extract(PlanDetail pl) {
		for (Block b : pl.getBlocks()) {
			if (b.getBuilding() != null && b.getBuilding().getFloors() != null
					&& !b.getBuilding().getFloors().isEmpty()) {
				for (Floor floor : b.getBuilding().getFloors()) {
					if (configCacheService.isUnitLayerEnabled()) {
						extractUnitWiseVerandah(pl, b, floor);
					} else {
						extractFloorWiseVerandah(pl, b, floor);
					}
				}
			}
		}
		return pl;
	}

	private void extractFloorWiseVerandah(PlanDetail pl, Block block, Floor floor) {
		String verandahLayer = String.format(layerNames.getLayerName(
				"LAYER_NAME_VERANDAH"), block.getNumber(), floor.getNumber());
		extractVerandah(pl, verandahLayer, floor.getVerandah());
	}

	private void extractUnitWiseVerandah(PlanDetail pl, Block block, Floor floor) {
		if (floor.getUnits() == null) {
			return;
		}
		for (FloorUnit floorUnit : floor.getUnits()) {
			if (floorUnit.getUnitNumber() == null) {
				continue;
			}
			String verandahLayer = String.format(layerNames.getLayerName(
					"LAYER_NAME_UNIT_VERANDAH"), block.getNumber(), floor.getNumber(), floorUnit.getUnitNumber());

			extractVerandah(pl, verandahLayer, floorUnit.getVerandah());
		}
	}

	private void extractVerandah(PlanDetail pl, String verandahLayer, MeasurementWithHeight verandahObj) {
		List<DXFLWPolyline> verandahs = Util.getPolyLinesByLayer(pl.getDoc(), verandahLayer);
		if (!verandahs.isEmpty()) {
			List<Measurement> verandahMeasurements = verandahs.stream()
					.map(polyline -> new MeasurementDetail(polyline, true))
					.collect(Collectors.toList());
			verandahObj.setMeasurements(verandahMeasurements);
			verandahObj.setHeightOrDepth(Util.getListOfDimensionValueByLayer(pl, verandahLayer));
		}
	}
}



//					List<DXFLWPolyline> verandahs = Util.getPolyLinesByLayer(pl.getDoc(),
//							String.format(layerNames.getLayerName("LAYER_NAME_VERANDAH"),b.getNumber(), f.getNumber()));
//					if (!verandahs.isEmpty()) {
//						List<Measurement> verandahMeasurements = verandahs.stream()
//								.map(polyline -> new MeasurementDetail(polyline, true)).collect(Collectors.toList());
//						f.getVerandah().setMeasurements(verandahMeasurements);
//
//						f.getVerandah().setHeightOrDepth((Util.getListOfDimensionValueByLayer(pl,
//										String.format(layerNames.getLayerName("LAYER_NAME_VERANDAH"),
//												b.getNumber(), f.getNumber()))));
//
//					}
//
//				}
//			}
//		}
//
//		return pl;
//	}
