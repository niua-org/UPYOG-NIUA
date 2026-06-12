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
import org.egov.edcr.service.ConfigCacheService;
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
    @Autowired
    private ConfigCacheService configCacheService;

    @Override
    public PlanDetail extract(PlanDetail pl) {
        if (LOG.isDebugEnabled())
            LOG.debug("Starting of Kitchen room Extract......");
        if (pl == null || pl.getBlocks().isEmpty()) {
            return pl;
        }

        for (Block block : pl.getBlocks()) {

            if (block.getBuilding() == null || block.getBuilding().getFloors() == null) {
                continue;
            }
            outside:
            // Process each floor in the current block.
            for (Floor floor : block.getBuilding().getFloors()) {
                // If typical floors are configured, check whether this floor is a repetitive floor.
                if (!block.getTypicalFloor().isEmpty())
                    // Iterate through all typical floor mappings for this block.
                    for (TypicalFloor tp : block.getTypicalFloor())
                        // Continue only when current floor number is listed as a repetitive floor.
                        if (tp.getRepetitiveFloorNos().contains(floor.getNumber()))
                            // Search all floors to find the model floor for this typical floor mapping.
                            for (Floor allFloors : block.getBuilding().getFloors())
                                // Match the model floor number with the typical floor model floor number.
                                if (allFloors.getNumber().equals(tp.getModelFloorNo())) {

                                    // In floor-wise mode, copy kitchen directly from the model floor.
                                    if (!configCacheService.isUnitLayerEnabled() && allFloors.getKitchen() != null) {
                                        floor.setKitchen(allFloors.getKitchen());
                                        // Skip normal extraction because typical floor kitchen has been copied.
                                        continue outside;
                                    }

                                    // In unit-wise mode, copy kitchen from matching units of the model floor.
                                    if (configCacheService.isUnitLayerEnabled() && allFloors.getUnits() != null) {

                                        // If repetitive floor has no units, use units from the model floor.
                                        if(floor.getUnits()==null){
                                            floor.setUnits(allFloors.getUnits());
                                        }
                                        // Loop through units of the current repetitive floor.
//                                        for (FloorUnit targetUnit : floor.getUnits()) {
//                                            // Skip target unit when unit number is missing.
//                                            if (targetUnit.getUnitNumber() == null) {
//                                                continue;
//                                            }
//                                            // Loop through units of the model floor.
//                                            for (FloorUnit sourceUnit : allFloors.getUnits()) {
//                                                // Skip source unit when unit number is missing.
//                                                if (sourceUnit.getUnitNumber() == null) {
//                                                    continue;
//                                                }
//                                                // Copy kitchen when source and target unit numbers match.
//                                                if (targetUnit.getUnitNumber().equals(sourceUnit.getUnitNumber()) && sourceUnit.getKitchen() != null) {
//                                                    targetUnit.setKitchen(sourceUnit.getKitchen());
//                                                }
//                                            }
//                                        }

                                        // Skip normal extraction because unit-wise typical kitchen copy is complete.
                                        continue outside;
                                    }
                                }

                // If floor was not handled as a typical repetitive floor, extract from drawing layers.
                if (configCacheService.isUnitLayerEnabled()) {
                    extractUnitWiseKitchen(pl, block, floor);
                }
                else {
                    extractFloorWiseKitchen(pl, block, floor);
                }
            }
        }
            return pl;
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
