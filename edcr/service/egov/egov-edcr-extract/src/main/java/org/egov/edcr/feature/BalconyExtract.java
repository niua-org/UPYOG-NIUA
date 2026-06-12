package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.egov.common.entity.edcr.*;
import org.egov.edcr.service.ConfigCacheService;
import org.egov.edcr.service.LayerNames;
import org.egov.common.entity.edcr.Measurement;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.utility.DcrConstants;
import org.egov.edcr.utility.Util;
import org.egov.infra.admin.master.entity.AppConfigValues;
import org.egov.infra.admin.master.service.AppConfigValueService;
import org.kabeja.dxf.DXFLWPolyline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BalconyExtract extends FeatureExtract {
    private static final Logger log = LoggerFactory.getLogger(BalconyExtract.class);
    @Autowired
    private LayerNames layerNames;

    @Autowired
    private AppConfigValueService appConfigValueService;

    @Autowired
    private ConfigCacheService configCacheService;

    @Override
    public PlanDetail validate(PlanDetail planDetail) {
        return planDetail;
    }

    @Override
    public PlanDetail extract(PlanDetail planDetail) {
        if (planDetail == null || planDetail.getBlocks() == null) {
            return planDetail;
        }


        for (Block block : planDetail.getBlocks()) {
            if (block.getBuilding() == null || block.getBuilding().getFloors() == null) {
                continue;
            }

            for (Floor floor : block.getBuilding().getFloors()) {
                if (configCacheService.isUnitLayerEnabled()) {
                    extractUnitWiseBalconies(planDetail, block, floor);
                } else {
                    extractFloorWiseBalconies(planDetail, block, floor);
                }
            }
        }

        return planDetail;
    }

    private void extractFloorWiseBalconies(PlanDetail planDetail, Block block, Floor floor) {
        String balconyLayerPattern = String.format(layerNames.getLayerName("LAYER_NAME_BLK_FLR_BALCONY"), block.getNumber(), floor.getNumber());
        floor.setBalconies(extractBalconies(planDetail, balconyLayerPattern));
    }

    private void extractUnitWiseBalconies(PlanDetail planDetail, Block block, Floor floor) {
        if (floor.getUnits() == null) {
            return;
        }

        for (FloorUnit floorUnit : floor.getUnits()) {
            if (floorUnit.getUnitNumber() == null) {
                continue;
            }

            log.info("Layer name in balcony extract " + layerNames.getLayerName("LAYER_NAME_BLK_FLR_UNIT_BALCONY"));

            String balconyLayerPattern = String.format(layerNames.getLayerName("LAYER_NAME_BLK_FLR_UNIT_BALCONY"),
                    block.getNumber(),
                    floor.getNumber(),
                    floorUnit.getUnitNumber());
//            String balconyLayerPattern = "BLK_1_FLR_0_UNIT_1_BALCONY_1";
            floorUnit.setBalconies(extractBalconies(planDetail, balconyLayerPattern));
        }
    }

    private List<Balcony> extractBalconies(PlanDetail planDetail, String balconyLayerPattern) {
        List<Balcony> balconies = new ArrayList<>();
        List<String> balconyLayers = Util.getLayerNamesLike(planDetail.getDoc(), balconyLayerPattern);

        for (String balconyLayer : balconyLayers) {
            List<DXFLWPolyline> balconyPolyLines = Util.getPolyLinesByLayer(planDetail.getDoc(), balconyLayer);
            List<BigDecimal> dimensions = Util.getListOfDimensionValueByLayer(planDetail, balconyLayer);

            if (dimensions.isEmpty() && balconyPolyLines.isEmpty()) {
                continue;
            }

            Balcony balcony = new Balcony();
            List<Measurement> balconyMeasurements = balconyPolyLines.stream()
                    .map(balconyPolyLine -> new MeasurementDetail(balconyPolyLine, true))
                    .collect(Collectors.toList());

            balcony.setMeasurements(balconyMeasurements);
            balcony.setWidths(dimensions);
            balcony.setNumber(getBalconyNumber(balconyLayer));
            balconies.add(balcony);
        }

        return balconies;
    }

    private String getBalconyNumber(String balconyLayer) {
        String[] layerParts = balconyLayer.split("_");
        return layerParts[layerParts.length - 1];
    }
}
