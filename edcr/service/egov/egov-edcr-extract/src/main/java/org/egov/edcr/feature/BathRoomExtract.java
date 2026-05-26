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
import org.kabeja.dxf.DXFLWPolyline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BathRoomExtract extends FeatureExtract {
    private static final Logger LOG = LogManager.getLogger(BathRoomExtract.class);
    @Autowired
    private LayerNames layerNames;

    @Autowired
    private AppConfigValueService appConfigValueService;

    @Override
    public PlanDetail validate(PlanDetail planDetail) {
        return planDetail;
    }

    @Override
    public PlanDetail extract(PlanDetail planDetail) {
        if (planDetail == null || planDetail.getBlocks() == null) {
            return planDetail;
        }

        boolean unitLayerEnabled = isUnitLayerEnabled();

//        List<DXFLWPolyline> rooms;
//        List<Measurement> roomMeasurements;
//        List<BigDecimal> roomHeights;
//        List<RoomHeight> roomHeightsList;
//        RoomHeight height;
        for (Block block : planDetail.getBlocks()) {
            if (block.getBuilding() == null || block.getBuilding().getFloors() == null) {
                continue;
            }

            for (Floor floor : block.getBuilding().getFloors()) {
                if (unitLayerEnabled) {
                    extractUnitWiseBathRooms(planDetail, block, floor);
                } else {
                    extractFloorWiseBathRooms(planDetail, block, floor);
                }
            }
        }
        return planDetail;
    }
    private boolean isUnitLayerEnabled(){
        List<AppConfigValues> appConfigValues = appConfigValueService.getConfigValuesByModuleAndKey(
                DcrConstants.APPLICATION_MODULE_TYPE, DcrConstants.FLOOR_UNIT_LAYER_ENABLED);

        return appConfigValues != null && !appConfigValues.isEmpty()
                && DcrConstants.YES.equalsIgnoreCase(appConfigValues.get(0).getValue());
    }
    private void extractFloorWiseBathRooms(PlanDetail planDetail,Block block,Floor floor) {
        String bathLayerName = String.format(layerNames.getLayerName("LAYER_NAME_BLK_FLR_BATH"), block.getNumber(), floor.getNumber());

        String bathHeightLayerName = String.format(layerNames.getLayerName("LAYER_NAME_BLK_FLR_BATH_HT"), block.getNumber(), floor.getNumber());

        floor.setBathRoom(extractBathRoom(planDetail, bathLayerName, bathHeightLayerName));
    }
    private void extractUnitWiseBathRooms(PlanDetail planDetail, Block block, Floor floor) {

        if (floor.getUnits() == null) {
            return;
        }

        for (FloorUnit floorUnit : floor.getUnits()) {

            if (floorUnit.getUnitNumber() == null) {
                continue;
            }

            LOG.info("Layer name in balcony extract " + layerNames.getLayerName("LAYER_NAME_BLK_FLR_UNIT_BATH"));

            String bathLayerName = String.format(
                    layerNames.getLayerName("LAYER_NAME_BLK_FLR_UNIT_BATH"),
                    block.getNumber(),
                    floor.getNumber(),
                    floorUnit.getUnitNumber());

            String bathHeightLayerName = String.format(
                    layerNames.getLayerName("LAYER_NAME_BLK_FLR_UNIT_BATH_HT"),
                    block.getNumber(),
                    floor.getNumber(),
                    floorUnit.getUnitNumber());

            floorUnit.setBathRoom(
                    extractBathRoom(planDetail, bathLayerName, bathHeightLayerName));
        }
    }
    private Room extractBathRoom(PlanDetail planDetail, String bathLayerName, String bathHeightLayerName) {

        List<DXFLWPolyline> rooms = Util.getPolyLinesByLayer(planDetail.getDoc(), bathLayerName);
        List<BigDecimal> roomHeights = Util.getListOfDimensionValueByLayer(planDetail, bathHeightLayerName);
        if ((rooms == null || rooms.isEmpty())
                && (roomHeights == null || roomHeights.isEmpty())) {
            return null;
        }
        Room bathRoom = new Room();
        List<Measurement> roomMeasurements = rooms.stream()
                .map(roomPolyLine -> new MeasurementDetail(roomPolyLine, true))
                .collect(Collectors.toList());
        bathRoom.setRooms(roomMeasurements);

        List<RoomHeight> roomHeightsList = new ArrayList<>();

        for (BigDecimal heightValue : roomHeights) {
            RoomHeight roomHeight = new RoomHeight();
            roomHeight.setHeight(heightValue);
            roomHeightsList.add(roomHeight);
        }

        bathRoom.setHeights(roomHeightsList);

        return bathRoom;
    }
}


