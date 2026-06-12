package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.egov.common.entity.edcr.*;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.service.LayerNames;
import org.egov.edcr.utility.DcrConstants;
import org.egov.edcr.utility.Util;
import org.egov.infra.admin.master.service.AppConfigValueService;
import org.egov.infra.admin.master.entity.AppConfigValues;
import org.egov.edcr.service.ConfigCacheService;
import org.kabeja.dxf.DXFLWPolyline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BathRoomWaterClosetsExtract extends FeatureExtract {
    private static final Logger LOG = LogManager.getLogger(BathRoomWaterClosetsExtract.class);
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

            if (block.getBuilding() == null
                    || block.getBuilding().getFloors() == null) {
                continue;
            }

            for (Floor floor : block.getBuilding().getFloors()) {

                if (configCacheService.isUnitLayerEnabled()) {
                    extractUnitWiseBathRoomWC(planDetail, block, floor);
                } else {
                    extractFloorWiseBathRoomWC(planDetail, block, floor);
                }
            }
        }

        return planDetail;
    }



    private void extractFloorWiseBathRoomWC(PlanDetail planDetail, Block block, Floor floor) {
        String wcBathLayerName = String.format(
                layerNames.getLayerName("LAYER_NAME_BLK_FLR_WC_BATH"),
                block.getNumber(),
                floor.getNumber());

        String wcBathHeightLayerName = String.format(
                layerNames.getLayerName("LAYER_NAME_BLK_FLR_WC_BATH_HT"),
                block.getNumber(),
                floor.getNumber());
        floor.setBathRoomWaterClosets(extractBathRoomWaterCloset(planDetail, wcBathLayerName, wcBathHeightLayerName));
    }

    private void extractUnitWiseBathRoomWC(PlanDetail planDetail, Block block, Floor floor) {
        if (floor.getUnits() == null) {
            return;
        }
        for (FloorUnit floorUnit : floor.getUnits()) {
            if (floorUnit.getUnitNumber() == null) {
                continue;
            }
            String wcBathLayerName = String.format(layerNames.getLayerName("LAYER_NAME_BLK_FLR_UNIT_WC_BATH"),
                    block.getNumber(),
                    floor.getNumber(),
                    floorUnit.getUnitNumber());

            String wcBathHeightLayerName = String.format(layerNames.getLayerName("LAYER_NAME_BLK_FLR_UNIT_WC_BATH_HT"),
                    block.getNumber(),
                    floor.getNumber(),
                    floorUnit.getUnitNumber());

            floorUnit.setBathRoomWaterClosets(extractBathRoomWaterCloset(planDetail, wcBathLayerName, wcBathHeightLayerName));
        }
    }

    private Room extractBathRoomWaterCloset(PlanDetail planDetail, String wcBathLayerName, String wcBathHeightLayerName) {
        List<DXFLWPolyline> rooms = Util.getPolyLinesByLayer(planDetail.getDoc(), wcBathLayerName);

        List<BigDecimal> roomHeights = Util.getListOfDimensionValueByLayer(planDetail, wcBathHeightLayerName);
        if ((rooms == null || rooms.isEmpty())
                && (roomHeights == null || roomHeights.isEmpty())) {
            return null;
        }
        Room bathRoomWC = new Room();
        List<Measurement> roomMeasurements = rooms.stream()
                .map(roomPolyLine ->
                        new MeasurementDetail(roomPolyLine, true))
                .collect(Collectors.toList());
        bathRoomWC.setRooms(roomMeasurements);
        List<RoomHeight> roomHeightsList = new ArrayList<>();
        for (BigDecimal heightValue : roomHeights) {
            RoomHeight roomHeight = new RoomHeight();
            roomHeight.setHeight(heightValue);
            roomHeightsList.add(roomHeight);
        }
        bathRoomWC.setHeights(roomHeightsList);
        return bathRoomWC;
    }
}

