package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.egov.common.entity.edcr.*;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.service.LayerNames;
import org.egov.edcr.utility.DcrConstants;
import org.egov.edcr.utility.Util;
import org.kabeja.dxf.DXFLWPolyline;
import org.egov.infra.admin.master.entity.AppConfigValues;
import org.egov.infra.admin.master.service.AppConfigValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KitchenExtract extends FeatureExtract {
    private static final Logger LOG = LogManager.getLogger(KitchenExtract.class);
    @Autowired
    private LayerNames layerNames;

    @Autowired
    private AppConfigValueService appConfigValueService;

    @Override
    public PlanDetail extract(PlanDetail pl) {
        if (LOG.isDebugEnabled())
            LOG.debug("Starting of Kitchen room Extract......");
        if (pl == null && pl.getBlocks().isEmpty()){
            return pl;
        }

        boolean unitLayerEnabled= isUnitLayerEnabled();

        for (Block block : pl.getBlocks()) {

            if (block.getBuilding() == null || block.getBuilding().getFloors() == null) {
                continue;
            }
            outside:
            for(Floor floor: block.getBuilding().getFloors()){
                if (!block.getTypicalFloor().isEmpty()) {
                    for (TypicalFloor tp : block.getTypicalFloor()) {
                        if (tp.getRepetitiveFloorNos().contains(floor.getNumber())) {
                            for (Floor allFloors : block.getBuilding().getFloors()) {
                                if (allFloors.getNumber().equals(tp.getModelFloorNo())) {

                                    if (!unitLayerEnabled && allFloors.getKitchen() != null) {
                                        floor.setKitchen(allFloors.getKitchen());
                                        continue outside;
                                    }

                                    if (unitLayerEnabled && allFloors.getUnits() != null && floor.getUnits() != null) {
                                        for (FloorUnit targetUnit :
                                                floor.getUnits()) {
                                            if (targetUnit.getUnitNumber() == null) {
                                                continue;
                                            }
                                            for (FloorUnit sourceUnit : allFloors.getUnits()) {
                                                if (sourceUnit.getUnitNumber() == null) {
                                                    continue;
                                                }
                                                if (targetUnit.getUnitNumber().equals(sourceUnit.getUnitNumber()) && sourceUnit.getKitchen() != null) {
                                                    targetUnit.setKitchen(sourceUnit.getKitchen());
                                                }
                                            }
                                        }

                                        continue outside;
                                    }
                                }
                            }
                        }
                    }
                }

                if (unitLayerEnabled) {
                    extractUnitWiseKitchen(pl, block, floor);
                } else {
                    extractFloorWiseKitchen(pl, block, floor);
                }
            }
        }

        return pl;
    }
    private boolean isUnitLayerEnabled()
    {
        List<AppConfigValues> appConfigValues =
                appConfigValueService.getConfigValuesByModuleAndKey(DcrConstants.APPLICATION_MODULE_TYPE, DcrConstants.FLOOR_UNIT_LAYER_ENABLED);

        return appConfigValues != null && !appConfigValues.isEmpty() && DcrConstants.YES.equalsIgnoreCase(appConfigValues.get(0).getValue());
    }
    private void extractFloorWiseKitchen(PlanDetail pl,Block block,Floor floor) {
        String kitchenLayer = String.format(layerNames.getLayerName("LAYER_NAME_KITCHEN"), block.getNumber(),
                floor.getNumber());
        floor.setKitchen(extractKitchen(pl,kitchenLayer));
    }

    private void extractUnitWiseKitchen(PlanDetail pl,Block block,Floor floor){
        if (floor.getUnits() == null) {
            return;
        }
        for (FloorUnit floorUnit : floor.getUnits()) {
            if (floorUnit.getUnitNumber() == null) {
                continue;
            }
            String kitchenLayer = String.format(
                    layerNames.getLayerName(
                            "LAYER_NAME_BLK_FLR_UNIT_KITCHEN"),
                    block.getNumber(),
                    floor.getNumber(),
                    floorUnit.getUnitNumber());
            floorUnit.setKitchen(extractKitchen(pl, kitchenLayer));
        }
    }
    private Room extractKitchen(PlanDetail pl,String kitchenLayer){
        List<DXFLWPolyline> kitchenPolyLines = new ArrayList<>();
        List<BigDecimal> kitchenHeight = Util.getListOfDimensionValueByLayer(pl, kitchenLayer);
        List<DXFLWPolyline> residentialKitchenPolyLines = Util.getPolyLinesByLayerAndColor(pl.getDoc(),
                kitchenLayer, DxfFileConstants.RESIDENTIAL_KITCHEN_ROOM_COLOR, pl);
        List<DXFLWPolyline> residentialKitchenStorePolyLines = Util.getPolyLinesByLayerAndColor(pl.getDoc(),
                kitchenLayer, DxfFileConstants.RESIDENTIAL_KITCHEN_STORE_ROOM_COLOR, pl);
        List<DXFLWPolyline> residentialKitchenDiningPolyLines = Util.getPolyLinesByLayerAndColor(pl.getDoc(),
                kitchenLayer, DxfFileConstants.RESIDENTIAL_KITCHEN_DINING_ROOM_COLOR, pl);
        List<DXFLWPolyline> commercialKitchenPolyLines = Util.getPolyLinesByLayerAndColor(pl.getDoc(),
                kitchenLayer, DxfFileConstants.COMMERCIAL_KITCHEN_ROOM_COLOR, pl);
        List<DXFLWPolyline> commercialKitchenStorePolyLines = Util.getPolyLinesByLayerAndColor(pl.getDoc(),
                kitchenLayer, DxfFileConstants.COMMERCIAL_KITCHEN_STORE_ROOM_COLOR, pl);
        List<DXFLWPolyline> commercialKitchenDiningPolyLines = Util.getPolyLinesByLayerAndColor(pl.getDoc(),
                kitchenLayer, DxfFileConstants.COMMERCIAL_KITCHEN_DINING_ROOM_COLOR, pl);

        if (!residentialKitchenPolyLines.isEmpty())
            kitchenPolyLines.addAll(residentialKitchenPolyLines);
        if (!residentialKitchenStorePolyLines.isEmpty())
            kitchenPolyLines.addAll(residentialKitchenStorePolyLines);
        if (!residentialKitchenDiningPolyLines.isEmpty())
            kitchenPolyLines.addAll(residentialKitchenDiningPolyLines);
        if (!commercialKitchenPolyLines.isEmpty())
            kitchenPolyLines.addAll(commercialKitchenPolyLines);
        if (!commercialKitchenStorePolyLines.isEmpty())
            kitchenPolyLines.addAll(commercialKitchenStorePolyLines);
        if (!commercialKitchenDiningPolyLines.isEmpty())
            kitchenPolyLines.addAll(commercialKitchenDiningPolyLines);
        if (kitchenHeight.isEmpty() && kitchenPolyLines.isEmpty()) {
            return null;
        }
        Room kitchen = new Room();
        List<RoomHeight> kitchenHeights = new ArrayList<>();
        for (BigDecimal height : kitchenHeight) {
            RoomHeight roomHeight = new RoomHeight();
            roomHeight.setHeight(height);
            kitchenHeights.add(roomHeight);
        }

        kitchen.setHeights(kitchenHeights);
        List<Measurement> kitchens = kitchenPolyLines.stream().map(acRoomPolyLine ->
                                new MeasurementDetail(acRoomPolyLine, true))
                .collect(Collectors.toList());kitchen.setRooms(kitchens);

        return kitchen;
    }
    @Override
    public PlanDetail validate(PlanDetail pl) {
        return pl;
    }
}