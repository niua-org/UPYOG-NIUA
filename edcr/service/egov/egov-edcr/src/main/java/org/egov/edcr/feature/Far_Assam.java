/*
 * UPYOG  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.edcr.feature;

import static org.egov.edcr.constants.CommonFeatureConstants.INT_ZERO;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_FIFTEEN;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ONE;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ONE_POINT_FIVE;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ONE_POINT_TWO;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_THREE;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_TWO;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_TWO_POINT_FIVE;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ZERO_POINT_FIVE;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ZERO_POINT_FOUR;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ZERO_POINT_SEVEN;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ZERO_POINT_SIX;
import static org.egov.edcr.constants.CommonFeatureConstants.LESS_THAN_EQUAL_TO_ZERO_POINT_TWO;
import static org.egov.edcr.constants.CommonKeyConstants.CARPET_AREA_BLOCK;
import static org.egov.edcr.constants.CommonKeyConstants.CARPET_AREA_NOT_DEFINED_BLOCK;
import static org.egov.edcr.constants.CommonKeyConstants.EXISTING_BUILT_UP_AREA;
import static org.egov.edcr.constants.CommonKeyConstants.EXISTING_CARPET_AREA_BLOCK;
import static org.egov.edcr.constants.CommonKeyConstants.EXISTING_CARPET_AREA_NOT_DEFINED;
import static org.egov.edcr.constants.CommonKeyConstants.EXISTING_FLOOR_AREA;
import static org.egov.edcr.constants.CommonKeyConstants.EXISTING_FLOOR_AREA_BLOCK;
import static org.egov.edcr.constants.CommonKeyConstants.EXISTING_FLOOR_LESS_CARPET_AREA;
import static org.egov.edcr.constants.CommonKeyConstants.FLOOR_AREA_BLOCK;
import static org.egov.edcr.constants.CommonKeyConstants.FLOOR_AREA_LESS_THAN_CARPET_AREA;
import static org.egov.edcr.constants.CommonKeyConstants.FLOOR_SPACED;
import static org.egov.edcr.constants.CommonKeyConstants.TOTAL_BUILDUP_AREA;
import static org.egov.edcr.constants.CommonKeyConstants.TOTAL_FLOOR_AREA;
import static org.egov.edcr.constants.DxfFileConstants.A;
import static org.egov.edcr.constants.DxfFileConstants.A2;
import static org.egov.edcr.constants.DxfFileConstants.A_AF;
import static org.egov.edcr.constants.DxfFileConstants.A_AF_GH;
import static org.egov.edcr.constants.DxfFileConstants.A_FH;
import static org.egov.edcr.constants.DxfFileConstants.A_R;
import static org.egov.edcr.constants.DxfFileConstants.A_SA;
import static org.egov.edcr.constants.DxfFileConstants.B;
import static org.egov.edcr.constants.DxfFileConstants.B2;
import static org.egov.edcr.constants.DxfFileConstants.D;
import static org.egov.edcr.constants.DxfFileConstants.D_A;
import static org.egov.edcr.constants.DxfFileConstants.D_B;
import static org.egov.edcr.constants.DxfFileConstants.D_C;
import static org.egov.edcr.constants.DxfFileConstants.E_CLG;
import static org.egov.edcr.constants.DxfFileConstants.E_EARC;
import static org.egov.edcr.constants.DxfFileConstants.E_NS;
import static org.egov.edcr.constants.DxfFileConstants.E_PS;
import static org.egov.edcr.constants.DxfFileConstants.E_SACA;
import static org.egov.edcr.constants.DxfFileConstants.E_SFDAP;
import static org.egov.edcr.constants.DxfFileConstants.E_SFMC;
import static org.egov.edcr.constants.DxfFileConstants.F;
import static org.egov.edcr.constants.DxfFileConstants.G;
import static org.egov.edcr.constants.DxfFileConstants.G_LI;
import static org.egov.edcr.constants.DxfFileConstants.G_NPHI;
import static org.egov.edcr.constants.DxfFileConstants.G_PHI;
import static org.egov.edcr.constants.DxfFileConstants.G_SI;
import static org.egov.edcr.constants.DxfFileConstants.H;
import static org.egov.edcr.constants.DxfFileConstants.H_PP;
import static org.egov.edcr.constants.DxfFileConstants.J;
import static org.egov.edcr.constants.DxfFileConstants.M_DFPAB;
import static org.egov.edcr.constants.DxfFileConstants.M_HOTHC;
import static org.egov.edcr.constants.DxfFileConstants.M_NAPI;
import static org.egov.edcr.constants.DxfFileConstants.M_OHF;
import static org.egov.edcr.constants.DxfFileConstants.M_VH;
import static org.egov.edcr.constants.DxfFileConstants.S_BH;
import static org.egov.edcr.constants.DxfFileConstants.S_CA;
import static org.egov.edcr.constants.DxfFileConstants.S_CRC;
import static org.egov.edcr.constants.DxfFileConstants.S_ECFG;
import static org.egov.edcr.constants.DxfFileConstants.S_ICC;
import static org.egov.edcr.constants.DxfFileConstants.S_MCH;
import static org.egov.edcr.constants.DxfFileConstants.S_SAS;
import static org.egov.edcr.constants.DxfFileConstants.S_SC;
import static org.egov.edcr.constants.EdcrReportConstants.FIFTEEN;
import static org.egov.edcr.constants.EdcrReportConstants.ONEBIGHA;
import static org.egov.edcr.constants.EdcrReportConstants.NEW;
import static org.egov.edcr.constants.EdcrReportConstants.NEW_AREA_ERROR;
import static org.egov.edcr.constants.EdcrReportConstants.NEW_AREA_ERROR_MSG;
import static org.egov.edcr.constants.EdcrReportConstants.OLD;
import static org.egov.edcr.constants.EdcrReportConstants.OLD_AREA_ERROR;
import static org.egov.edcr.constants.EdcrReportConstants.OLD_AREA_ERROR_MSG;
import static org.egov.edcr.constants.EdcrReportConstants.ONE;
import static org.egov.edcr.constants.EdcrReportConstants.ONE_POINTFIVE;
import static org.egov.edcr.constants.EdcrReportConstants.ONE_POINTTWO;
import static org.egov.edcr.constants.EdcrReportConstants.POINTFIVE;
import static org.egov.edcr.constants.EdcrReportConstants.POINTTHREE;
import static org.egov.edcr.constants.EdcrReportConstants.POINTFOUR;
import static org.egov.edcr.constants.EdcrReportConstants.POINTSEVEN;
import static org.egov.edcr.constants.EdcrReportConstants.POINTSIX;
import static org.egov.edcr.constants.EdcrReportConstants.POINTTWO;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_EIGHTEEN_POINTTHREE;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_FOUR_POINTEIGHT;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_NINE_POINTONE;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_SIX_POINTONE;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_THIRTY_POINTFIVE;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_THREE_POINTSIX;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_TWELVE_POINTTWO;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_TWENTYFOUR_POINTFOUR;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_TWENTYSEVEN_POINTFOUR;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_TWO_POINTFOUR;
import static org.egov.edcr.constants.EdcrReportConstants.ROAD_WIDTH_TWO_POINTFOURFOUR;
import static org.egov.edcr.constants.EdcrReportConstants.RULE_38;
import static org.egov.edcr.constants.EdcrReportConstants.THREE;
import static org.egov.edcr.constants.EdcrReportConstants.THREE_POINTFIVE;
import static org.egov.edcr.constants.EdcrReportConstants.TWO;
import static org.egov.edcr.constants.EdcrReportConstants.TWOTHOUSAND;
import static org.egov.edcr.constants.EdcrReportConstants.EWS;
import static org.egov.edcr.constants.EdcrReportConstants.LIG;
import static org.egov.edcr.constants.EdcrReportConstants.TWO_POINTFIVE;
import static org.egov.edcr.constants.EdcrReportConstants.POINTTWOFIVE;
import static org.egov.edcr.constants.EdcrReportConstants.VALIDATION_NEGATIVE_BUILTUP_AREA;
import static org.egov.edcr.constants.EdcrReportConstants.VALIDATION_NEGATIVE_EXISTING_BUILTUP_AREA;
import static org.egov.edcr.constants.EdcrReportConstants.VALIDATION_NEGATIVE_EXISTING_FLOOR_AREA;
import static org.egov.edcr.constants.EdcrReportConstants.VALIDATION_NEGATIVE_FLOOR_AREA;
import static org.egov.edcr.service.FeatureUtil.addScrutinyDetailtoPlan;
import static org.egov.edcr.service.FeatureUtil.mapReportDetails;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.constants.MdmsFeatureConstants;
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.FarDetails;
import org.egov.common.entity.edcr.FarRequirement;
import org.egov.common.entity.edcr.FeatureEnum;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ReportScrutinyDetail;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.service.MDMSCacheManager;
import org.egov.edcr.service.ProcessPrintHelper;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Far_Assam extends Far {
	private static final Logger LOG = LogManager.getLogger(Far_Assam.class);
	BigDecimal totalExistingBuiltUpArea = BigDecimal.ZERO;
	BigDecimal totalExistingFloorArea = BigDecimal.ZERO;
	BigDecimal totalBuiltUpArea = BigDecimal.ZERO;
	BigDecimal totalFloorArea = BigDecimal.ZERO;
	BigDecimal totalCarpetArea = BigDecimal.ZERO;
	BigDecimal totalExistingCarpetArea = BigDecimal.ZERO;// Use an appropriate																// upper bound

	@Autowired
	MDMSCacheManager cache;
	
	
	
	/**
	 * Validates the given Plan object to ensure the plot area is defined and greater than zero.
	 *
	 * @param pl The Plan object to validate.
	 * @return The validated Plan object with any validation errors added.
	 */
	@Override
	public Plan validate(Plan pl) {
		if (pl.getPlot() == null || (pl.getPlot() != null
				&& (pl.getPlot().getArea() == null || pl.getPlot().getArea().doubleValue() == 0))) {
			pl.addError(PLOT_AREA, getLocaleMessage(OBJECTNOTDEFINED, PLOT_AREA));
			
		}
      
		return pl;
	}

	/**
	 * Processes the given Plan object to calculate various building metrics like FAR,
	 * total built-up area, carpet area, etc., and populate the plan details accordingly.
	 *
	 * @param pl The Plan object to be processed.
	 * @return The updated Plan object with computed values and any processing errors.
	 */
	@Override
	public Plan process(Plan pl) {
	    LOG.info("Starting process method");

	    decideNocIsRequired(pl);

	    LOG.debug("Called decideNocIsRequired");

	    System.out.println("hi inside process");

	    HashMap<String, String> errorMsgs = new HashMap<>();
	    int initialErrorCount = pl.getErrors().size();
	    LOG.debug("Initial error count: {}", initialErrorCount);

	    validate(pl);
	    LOG.debug("Called validate method");

	    System.out.println("plotarea" + pl.getPlot().getArea());

	    if (validationFailed(pl, initialErrorCount)) {
	        LOG.warn("Validation failed with new errors added. Returning plan early.");
	        return pl;
	    }

	    Set<OccupancyTypeHelper> distinctOccupancyTypesHelper = new HashSet<>();

	    processAllBlockOccupancies(pl);
	    LOG.debug("Completed processAllBlockOccupancies");

	    processBlocks(pl);
	    LOG.debug("Completed processBlocks");

	    Set<OccupancyTypeHelper> setOfDistinctOccupancyTypes = collectDistinctOccupancyTypes(pl);
	    LOG.debug("Distinct occupancy types collected: count={}", setOfDistinctOccupancyTypes.size());

	    distinctOccupancyTypesHelper.addAll(setOfDistinctOccupancyTypes);

	    List<Occupancy> occupanciesForPlan = collectOccupanciesForPlan(setOfDistinctOccupancyTypes, pl);
	    pl.setOccupancies(occupanciesForPlan);
	    LOG.debug("Set occupancies for plan: count={}", occupanciesForPlan.size());

	    populatePlanAndVirtualBuildingDetails(pl, setOfDistinctOccupancyTypes, distinctOccupancyTypesHelper,
	            totalFloorArea, totalCarpetArea, totalExistingBuiltUpArea, totalExistingFloorArea,
	            totalExistingCarpetArea, totalBuiltUpArea);
	    LOG.debug("Populated plan and virtual building details");

	    processOccupancyInformation(pl);
	    LOG.debug("Processed occupancy information on plan");

	    BigDecimal surrenderRoadArea = calculateSurrenderRoadArea(pl);
	    pl.setTotalSurrenderRoadArea(surrenderRoadArea.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
	            DcrConstants.ROUNDMODE_MEASUREMENTS));
	    LOG.debug("Calculated surrender road area: {}", surrenderRoadArea);

	    BigDecimal plotArea = calculateTotalPlotArea(pl, surrenderRoadArea);
	    LOG.debug("Calculated total plot area (including surrender road): {}", plotArea);

	    BigDecimal providedFar = calculateProvidedFar(pl, plotArea);
	    pl.setFarDetails(new FarDetails());
	    pl.getFarDetails().setProvidedFar(providedFar.doubleValue());
	    LOG.debug("Calculated and set provided FAR: {}", providedFar);

	    processFarComputation(pl, providedFar, plotArea, errorMsgs);
	    LOG.debug("Completed FAR computation");

	    ProcessPrintHelper.print(pl);
	    LOG.info("Completed process method");

	    return pl;
	}

	/**
	 * Checks if any new validation errors have been added since the initial error count.
	 */
	private boolean validationFailed(Plan pl, int initialErrorCount) {
	    int validatedErrors = pl.getErrors().size();
	    LOG.debug("Validating errors: before={}, after={}", initialErrorCount, validatedErrors);
	    if (validatedErrors > initialErrorCount) {
	        System.out.println("hi inside error");
	        System.out.println("error" + pl.getErrors().get(PLOT_AREA));
	        LOG.warn("New validation errors detected.");
	        return true;
	    }
	    return false;
	}

	/**
	 * Collects all distinct occupancy types used across all blocks in the plan.
	 */
	private Set<OccupancyTypeHelper> collectDistinctOccupancyTypes(Plan pl) {
	    List<OccupancyTypeHelper> plotWiseOccupancyTypes = new ArrayList<>();
	    for (Block block : pl.getBlocks()) {
	        for (Occupancy occupancy : block.getBuilding().getOccupancies()) {
	            if (occupancy.getTypeHelper() != null) {
	                plotWiseOccupancyTypes.add(occupancy.getTypeHelper());
	            }
	        }
	    }
	    Set<OccupancyTypeHelper> distinctSet = new HashSet<>(plotWiseOccupancyTypes);
	    LOG.debug("Collected distinct occupancy types count: {}", distinctSet.size());
	    return distinctSet;
	}

	/**
	 * Populates the Plan's occupancy information based on virtual building occupancy types.
	 */
	private void processOccupancyInformation(Plan pl) {
	    if (pl.getVirtualBuilding() != null && !pl.getVirtualBuilding().getOccupancyTypes().isEmpty()) {
	        List<String> occupancies = new ArrayList<>();
	        pl.getVirtualBuilding().getOccupancyTypes().forEach(occ -> {
	            if (occ.getType() != null)
	                occupancies.add(occ.getType().getName());
	        });

	        Set<String> distinctOccupancies = new HashSet<>(occupancies);
	        pl.getPlanInformation()
	                .setOccupancy(distinctOccupancies.stream().map(String::new).collect(Collectors.joining(",")));

	        LOG.debug("Processed occupancy information. Distinct occupancies: {}", distinctOccupancies);
	    } else {
	        LOG.debug("No virtual building occupancy types to process.");
	    }
	}

	/**
	 * Calculates the total surrender road area for the plan.
	 */
	private BigDecimal calculateSurrenderRoadArea(Plan pl) {
	    BigDecimal surrenderRoadArea = BigDecimal.ZERO;
	    if (!pl.getSurrenderRoads().isEmpty()) {
	        for (Measurement measurement : pl.getSurrenderRoads()) {
	            surrenderRoadArea = surrenderRoadArea.add(measurement.getArea());
	        }
	    }
	    LOG.debug("Calculated surrender road area: {}", surrenderRoadArea);
	    return surrenderRoadArea;
	}

	/**
	 * Calculates the total plot area including surrender road area.
	 */
	private BigDecimal calculateTotalPlotArea(Plan pl, BigDecimal surrenderRoadArea) {
	    BigDecimal totalPlotArea = pl.getPlot() != null ? pl.getPlot().getArea().add(surrenderRoadArea) : BigDecimal.ZERO;
	    LOG.debug("Calculated total plot area: {}", totalPlotArea);
	    return totalPlotArea;
	}

	/**
	 * Calculates the provided Floor Area Ratio (FAR).
	 */
	private BigDecimal calculateProvidedFar(Plan pl, BigDecimal plotArea) {
	    if (plotArea.doubleValue() > 0) {
	        BigDecimal far = pl.getVirtualBuilding().getTotalFloorArea().divide(plotArea,
	                DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
	        LOG.debug("Calculated FAR: {}", far);
	        return far;
	    }
	    LOG.debug("Plot area is zero or negative, FAR set to zero");
	    return BigDecimal.ZERO;
	}

	/**
	 * Computes and validates the FAR for the given plan.
	 */
	private void processFarComputation(Plan pl, BigDecimal providedFar, BigDecimal plotArea, HashMap<String, String> errorMsgs) {
	    OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding() != null
	            ? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
	            : null;

	    String typeOfArea = pl.getPlanInformation().getTypeOfArea();
	    BigDecimal roadWidth = pl.getPlanInformation().getRoadWidth();
	    String feature = MdmsFeatureConstants.FAR;

	    LOG.debug("Processing FAR computation with parameters - MostRestrictiveOccupancyType: {}, TypeOfArea: {}, RoadWidth: {}",
	            mostRestrictiveOccupancyType, typeOfArea, roadWidth);

	    if (mostRestrictiveOccupancyType != null && StringUtils.isNotBlank(typeOfArea) && roadWidth != null
	            && !processFarForSpecialOccupancy(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth,
	                    errorMsgs)) {
	        processFar(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth, errorMsgs,
	                feature, mostRestrictiveOccupancyType.getType().getName());
	        LOG.debug("Processed FAR for normal occupancy");
	    } else {
	        processFarIndustrial(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth, errorMsgs,
	                feature, mostRestrictiveOccupancyType.getType().getName());
	        LOG.debug("Processed FAR for industrial occupancy");
	    }
	}

	/**
	 * Iterates over all blocks and processes each block's occupancy details.
	 */
	private void processAllBlockOccupancies(Plan pl) {
	    LOG.debug("Start processing all block occupancies");

	    for (Block blk : pl.getBlocks()) {
	        processBlockOccupancies(pl, blk);
	    }

	    LOG.debug("Completed processing all block occupancies");
	}

	/**
	 * Processes occupancy details for a specific block and updates Plan totals.
	 */
	private void processBlockOccupancies(Plan pl, Block blk) {
	    LOG.debug("Processing block occupancies for Block Number: {}", blk.getNumber());

	    BigDecimal flrArea = BigDecimal.ZERO;
	    BigDecimal bltUpArea = BigDecimal.ZERO;
	    BigDecimal existingFlrArea = BigDecimal.ZERO;
	    BigDecimal existingBltUpArea = BigDecimal.ZERO;
	    BigDecimal carpetArea = BigDecimal.ZERO;
	    BigDecimal existingCarpetArea = BigDecimal.ZERO;

	    Building building = blk.getBuilding();

	    for (Floor flr : building.getFloors()) {
	        for (Occupancy occupancy : flr.getOccupancies()) {
	            validate2(pl, blk, flr, occupancy);

	            bltUpArea = bltUpArea.add(occupancy.getBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getBuiltUpArea());
	            existingBltUpArea = existingBltUpArea.add(occupancy.getExistingBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getExistingBuiltUpArea());
	            flrArea = flrArea.add(occupancy.getFloorArea());
	            existingFlrArea = existingFlrArea.add(occupancy.getExistingFloorArea());
	            carpetArea = carpetArea.add(occupancy.getCarpetArea());
	            existingCarpetArea = existingCarpetArea.add(occupancy.getExistingCarpetArea());
	        }
	    }

	    building.setTotalFloorArea(flrArea);
	    building.setTotalBuitUpArea(bltUpArea);
	    building.setTotalExistingBuiltUpArea(existingBltUpArea);
	    building.setTotalExistingFloorArea(existingFlrArea);

	    if (existingBltUpArea.compareTo(bltUpArea) == 0)
	        blk.setCompletelyExisting(Boolean.TRUE);

	    totalFloorArea = totalFloorArea.add(flrArea);
	    totalBuiltUpArea = totalBuiltUpArea.add(bltUpArea);
	    totalExistingBuiltUpArea = totalExistingBuiltUpArea.add(existingBltUpArea);
	    totalExistingFloorArea = totalExistingFloorArea.add(existingFlrArea);
	    totalCarpetArea = totalCarpetArea.add(carpetArea);
	    totalExistingCarpetArea = totalExistingCarpetArea.add(existingCarpetArea);

	    LOG.debug("Block Number: {} area totals updated - FloorArea: {}, BuiltUpArea: {}, ExistingFloorArea: {}, ExistingBuiltUpArea: {}",
	            blk.getNumber(), flrArea, bltUpArea, existingFlrArea, existingBltUpArea);

	    processBlockOccupancyTypes(blk);
	    LOG.debug("Completed processBlockOccupancyTypes for Block Number: {}", blk.getNumber());
	}

	/**
	 * Processes and categorizes occupancy types block-wise and computes their aggregated areas.
	 */
	private void processBlockOccupancyTypes(Block blk) {
	    LOG.debug("Processing occupancy types for Block Number: {}", blk.getNumber());

	    Set<OccupancyTypeHelper> occupancyByBlock = new HashSet<>();
	    for (Floor flr : blk.getBuilding().getFloors()) {
	        for (Occupancy occupancy : flr.getOccupancies()) {
	            if (occupancy.getTypeHelper() != null) {
	                occupancyByBlock.add(occupancy.getTypeHelper());
	            }
	        }
	    }

	    List<Map<String, Object>> listOfMapOfAllDtls = new ArrayList<>();
	    List<OccupancyTypeHelper> listOfOccupancyTypes = new ArrayList<>();

	    for (OccupancyTypeHelper occupancyType : occupancyByBlock) {
	        Map<String, Object> allDtlsMap = new HashMap<>();
	        BigDecimal blockWiseFloorArea = BigDecimal.ZERO;
	        BigDecimal blockWiseBuiltupArea = BigDecimal.ZERO;
	        BigDecimal blockWiseExistingFloorArea = BigDecimal.ZERO;
	        BigDecimal blockWiseExistingBuiltupArea = BigDecimal.ZERO;

	        for (Floor flr : blk.getBuilding().getFloors()) {
	            for (Occupancy occupancy : flr.getOccupancies()) {
	                if (occupancyTypeMatches(occupancyType, occupancy)) {
	                    blockWiseFloorArea = blockWiseFloorArea.add(occupancy.getFloorArea());
	                    blockWiseBuiltupArea = blockWiseBuiltupArea.add(occupancy.getBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getBuiltUpArea());
	                    blockWiseExistingFloorArea = blockWiseExistingFloorArea.add(occupancy.getExistingFloorArea());
	                    blockWiseExistingBuiltupArea = blockWiseExistingBuiltupArea.add(occupancy.getExistingBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getExistingBuiltUpArea());
	                }
	            }
	        }

	        Occupancy occupancy = new Occupancy();
	        occupancy.setBuiltUpArea(blockWiseBuiltupArea);
	        occupancy.setFloorArea(blockWiseFloorArea);
	        occupancy.setExistingFloorArea(blockWiseExistingFloorArea);
	        occupancy.setExistingBuiltUpArea(blockWiseExistingBuiltupArea);
	        occupancy.setCarpetArea(blockWiseFloorArea.multiply(BigDecimal.valueOf(.80)));
	        occupancy.setTypeHelper(occupancyType);
	        blk.getBuilding().getTotalArea().add(occupancy);

	        allDtlsMap.put(OCCUPANCY, occupancyType);
	        allDtlsMap.put(TOTAL_FLOOR_AREA, blockWiseFloorArea);
	        allDtlsMap.put(TOTAL_BUILDUP_AREA, blockWiseBuiltupArea);
	        allDtlsMap.put(EXISTING_FLOOR_AREA, blockWiseExistingFloorArea);
	        allDtlsMap.put(EXISTING_BUILT_UP_AREA, blockWiseExistingBuiltupArea);

	        listOfOccupancyTypes.add(occupancyType);
	        listOfMapOfAllDtls.add(allDtlsMap);
	    }

	    LOG.debug("Occupancy types and details mapped for Block Number: {}. Occupancies count: {}", blk.getNumber(), listOfOccupancyTypes.size());

	    buildOccupancyListForBlock(blk, listOfOccupancyTypes, listOfMapOfAllDtls);
	}

	/**
	 * Checks if the given occupancy matches the occupancy type helper.
	 */
	private boolean occupancyTypeMatches(OccupancyTypeHelper type, Occupancy occ) {
	    boolean match = type.getType() != null && type.getType().getCode() != null
	            && occ.getTypeHelper() != null && occ.getTypeHelper().getType() != null
	            && occ.getTypeHelper().getType().getCode() != null
	            && occ.getTypeHelper().getType().getCode().equals(type.getType().getCode());
	    LOG.debug("Matching occupancy type. OccupancyTypeHelper code: {}, Occupancy code: {}, Match: {}",
	            type.getType().getCode(), occ.getTypeHelper() != null ? occ.getTypeHelper().getType().getCode() : null, match);
	    return match;
	}

	/**
	 * Builds a list of occupancy objects for a block, aggregates areas, and classifies the block.
	 */
	private void buildOccupancyListForBlock(Block blk, List<OccupancyTypeHelper> listOfOccupancyTypes,
	                                        List<Map<String, Object>> listOfMapOfAllDtls) {

	    Set<OccupancyTypeHelper> setOfOccupancyTypes = new HashSet<>(listOfOccupancyTypes);
	    List<Occupancy> listOfOccupanciesOfAParticularblock = new ArrayList<>();

	    for (OccupancyTypeHelper occupancyType : setOfOccupancyTypes) {
	        if (occupancyType != null) {
	            Occupancy occupancy = new Occupancy();
	            BigDecimal totalFlrArea = BigDecimal.ZERO;
	            BigDecimal totalBltUpArea = BigDecimal.ZERO;
	            BigDecimal totalExistingFlrArea = BigDecimal.ZERO;
	            BigDecimal totalExistingBltUpArea = BigDecimal.ZERO;

	            for (Map<String, Object> dtlsMap : listOfMapOfAllDtls) {
	                if (occupancyType.equals(dtlsMap.get(OCCUPANCY))) {
	                    totalFlrArea = totalFlrArea.add((BigDecimal) dtlsMap.get(TOTAL_FLOOR_AREA));
	                    totalBltUpArea = totalBltUpArea.add((BigDecimal) dtlsMap.get(TOTAL_BUILDUP_AREA));
	                    totalExistingBltUpArea = totalExistingBltUpArea.add((BigDecimal) dtlsMap.get(EXISTING_BUILT_UP_AREA));
	                    totalExistingFlrArea = totalExistingFlrArea.add((BigDecimal) dtlsMap.get(EXISTING_FLOOR_AREA));
	                }
	            }

	            occupancy.setTypeHelper(occupancyType);
	            occupancy.setBuiltUpArea(totalBltUpArea);
	            occupancy.setFloorArea(totalFlrArea);
	            occupancy.setExistingBuiltUpArea(totalExistingBltUpArea);
	            occupancy.setExistingFloorArea(totalExistingFlrArea);
	            occupancy.setExistingCarpetArea(totalExistingFlrArea.multiply(BigDecimal.valueOf(0.80)));
	            occupancy.setCarpetArea(totalFlrArea.multiply(BigDecimal.valueOf(0.80)));

	            listOfOccupanciesOfAParticularblock.add(occupancy);

	            LOG.debug("Built occupancy for type: {} with FloorArea: {}, BuiltUpArea: {}", occupancyType, totalFlrArea, totalBltUpArea);
	        }
	    }

	    blk.getBuilding().setOccupancies(listOfOccupanciesOfAParticularblock);
	    LOG.debug("Set occupancies for block Number: {} with count: {}", blk.getNumber(), listOfOccupanciesOfAParticularblock.size());
	    classifyBlock(blk, listOfOccupanciesOfAParticularblock);
	}

	/**
	 * Classifies the block based on occupancy types.
	 */
	private void classifyBlock(Block blk, List<Occupancy> listOfOccupancies) {
	    if (listOfOccupancies.isEmpty()) {
	        LOG.debug("No occupancies to classify for block Number: {}", blk.getNumber());
	        return;
	    }

	    boolean singleFamilyPresent = false;
	    boolean otherTypePresent = false;

	    for (Occupancy occ : listOfOccupancies) {
	        if (occ.getTypeHelper().getSubtype() != null && A_R.equals(occ.getTypeHelper().getSubtype().getCode())) {
	            singleFamilyPresent = true;
	        } else {
	            otherTypePresent = true;
	            break;
	        }
	    }

	    blk.setSingleFamilyBuilding(!otherTypePresent && singleFamilyPresent);
	    blk.setResidentialBuilding(isOnlyOfType(listOfOccupancies, A));
	    blk.setResidentialOrCommercialBuilding(isOnlyOfType(listOfOccupancies, A, F));

	    LOG.debug("Block Number: {} classification set - SingleFamily: {}, Residential: {}, ResidentialOrCommercial: {}",
	            blk.getNumber());
	}

	/**
	 * Checks whether the given occupancies only belong to specified allowed type codes.
	 */
	private boolean isOnlyOfType(List<Occupancy> occupancies, String... allowedTypes) {
	    Set<String> allowed = new HashSet<>(Arrays.asList(allowedTypes));
	    for (Occupancy occ : occupancies) {
	        if (occ.getTypeHelper() == null || occ.getTypeHelper().getType() == null
	                || !allowed.contains(occ.getTypeHelper().getType().getCode())) {
	            LOG.debug("Occupancy code not in allowed list: {}", occ.getTypeHelper() != null ? occ.getTypeHelper().getType().getCode() : "null");
	            return false;
	        }
	    }
	    return true;
	}

	/**
	 * Processes all blocks in the given plan:
	 * - Identifies most restrictive FAR for each block
	 * - Validates floor areas against carpet and built-up areas
	 */
	private void processBlocks(Plan pl) {
	    LOG.debug("Processing blocks to identify most restrictive FAR and validate areas");

	    for (Block blk : pl.getBlocks()) {
	        Building building = blk.getBuilding();
	        Set<OccupancyTypeHelper> setOfBlockDistinctOccupancyTypes = processBlockOccupancies(building);
	        OccupancyTypeHelper mostRestrictiveFar = getMostRestrictiveFar(setOfBlockDistinctOccupancyTypes);
	        building.setMostRestrictiveFarHelper(mostRestrictiveFar);

	        LOG.debug("Block Number: {} set most restrictive FAR: {}", blk.getNumber(), mostRestrictiveFar);

	        for (Floor flr : building.getFloors()) {
	            validateFloorAreas(pl, blk, flr, mostRestrictiveFar);
	        }
	    }
	}

	/**
	 * Extracts set of distinct occupancy types in a building.
	 */
	private Set<OccupancyTypeHelper> processBlockOccupancies(Building building) {
	    List<OccupancyTypeHelper> blockWiseOccupancyTypes = new ArrayList<>();
	    for (Occupancy occupancy : building.getOccupancies()) {
	        if (occupancy.getTypeHelper() != null) {
	            blockWiseOccupancyTypes.add(occupancy.getTypeHelper());
	        }
	    }
	    Set<OccupancyTypeHelper> occupancySet = new HashSet<>(blockWiseOccupancyTypes);
	    LOG.debug("Processed block occupancies, distinct count: {}", occupancySet.size());
	    return occupancySet;
	}

	/**
	 * Validates the floor area, carpet area, and built-up area of a floor against the
	 * most restrictive FAR subtype. Adds errors to the plan if validation fails.
	 *
	 * @param pl the plan to which validation errors are to be added
	 * @param blk the block containing the floor
	 * @param flr the floor being validated
	 * @param mostRestrictiveFar the most restrictive occupancy type helper for the block
	 */
	private void validateFloorAreas(Plan pl, Block blk, Floor flr, OccupancyTypeHelper mostRestrictiveFar) {
		BigDecimal flrArea = BigDecimal.ZERO;
		BigDecimal existingFlrArea = BigDecimal.ZERO;
		BigDecimal carpetArea = BigDecimal.ZERO;
		BigDecimal existingCarpetArea = BigDecimal.ZERO;
		BigDecimal existingBltUpArea = BigDecimal.ZERO;

		for (Occupancy occupancy : flr.getOccupancies()) {
			flrArea = flrArea.add(occupancy.getFloorArea());
			existingFlrArea = existingFlrArea.add(occupancy.getExistingFloorArea());
			carpetArea = carpetArea.add(occupancy.getCarpetArea());
			existingCarpetArea = existingCarpetArea.add(occupancy.getExistingCarpetArea());
		}

		for (Occupancy occupancy : flr.getOccupancies()) {
			existingBltUpArea = existingBltUpArea.add(
				occupancy.getExistingBuiltUpArea() != null ? occupancy.getExistingBuiltUpArea() : BigDecimal.ZERO);
		}

		if (mostRestrictiveFar != null && mostRestrictiveFar.getConvertedSubtype() != null
				&& !A_R.equals(mostRestrictiveFar.getSubtype().getCode())) {
			if (carpetArea.compareTo(BigDecimal.ZERO) == 0) {
				pl.addError(CARPET_AREA_BLOCK + blk.getNumber() + FLOOR_SPACED + flr.getNumber(),
						CARPET_AREA_NOT_DEFINED_BLOCK + blk.getNumber() + FLOOR_SPACED + flr.getNumber());
			}

			if (existingBltUpArea.compareTo(BigDecimal.ZERO) > 0
					&& existingCarpetArea.compareTo(BigDecimal.ZERO) == 0) {
				pl.addError(EXISTING_CARPET_AREA_BLOCK + blk.getNumber() + FLOOR_SPACED + flr.getNumber(),
						EXISTING_CARPET_AREA_NOT_DEFINED + blk.getNumber() + FLOOR_SPACED
								+ flr.getNumber());
			}
		}

		if (flrArea.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS, DcrConstants.ROUNDMODE_MEASUREMENTS)
				.compareTo(carpetArea.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
						DcrConstants.ROUNDMODE_MEASUREMENTS)) < 0) {
			pl.addError(FLOOR_AREA_BLOCK + blk.getNumber() + FLOOR_SPACED + flr.getNumber(),
					FLOOR_AREA_LESS_THAN_CARPET_AREA + blk.getNumber() + FLOOR_SPACED
							+ flr.getNumber());
		}

		if (existingBltUpArea.compareTo(BigDecimal.ZERO) > 0 && existingFlrArea
				.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS, DcrConstants.ROUNDMODE_MEASUREMENTS)
				.compareTo(existingCarpetArea.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
						DcrConstants.ROUNDMODE_MEASUREMENTS)) < 0) {
			pl.addError(EXISTING_FLOOR_AREA_BLOCK + blk.getNumber() + FLOOR_SPACED + flr.getNumber(),
					EXISTING_FLOOR_LESS_CARPET_AREA + blk.getNumber() + FLOOR_SPACED
							+ flr.getNumber());
		}
	}

	/**
	 * Collects and aggregates occupancy details for the given plan from the provided set of distinct occupancy types.
	 *
	 * @param setOfDistinctOccupancyTypes a set of distinct occupancy type helpers used in the plan
	 * @param pl the {@link Plan} object representing the building plan
	 * @return a list of {@link Occupancy} objects corresponding to the occupancy types in the plan
	 */

	private List<Occupancy> collectOccupanciesForPlan(Set<OccupancyTypeHelper> setOfDistinctOccupancyTypes, Plan pl) {
	    List<Occupancy> occupanciesForPlan = new ArrayList<>();
	    for (OccupancyTypeHelper occupancyType : setOfDistinctOccupancyTypes) {
	        if (occupancyType != null) {
	            occupanciesForPlan.add(aggregateOccupancyForType(occupancyType, pl));
	        }
	    }
	    return occupanciesForPlan;
	}
	
	/**
	 * Populates the plan object and its virtual building with calculated occupancy and area details.
	 *
	 * @param pl the {@link Plan} object to populate
	 * @param setOfDistinctOccupancyTypes all distinct occupancy types used in the plan
	 * @param distinctOccupancyTypesHelper filtered set of occupancy types used for type-specific validations
	 * @param totalFloorArea total floor area for all blocks
	 * @param totalCarpetArea total carpet area for all blocks
	 * @param totalExistingBuiltUpArea total existing built-up area
	 * @param totalExistingFloorArea total existing floor area
	 * @param totalExistingCarpetArea total existing carpet area
	 * @param totalBuiltUpArea total built-up area for the proposed construction
	 */
	private void populatePlanAndVirtualBuildingDetails(Plan pl, Set<OccupancyTypeHelper> setOfDistinctOccupancyTypes,
	        Set<OccupancyTypeHelper> distinctOccupancyTypesHelper, BigDecimal totalFloorArea,
	        BigDecimal totalCarpetArea, BigDecimal totalExistingBuiltUpArea, BigDecimal totalExistingFloorArea,
	        BigDecimal totalExistingCarpetArea, BigDecimal totalBuiltUpArea) {

	    setOccupanciesAndAreas(pl, setOfDistinctOccupancyTypes, distinctOccupancyTypesHelper,
	            totalFloorArea, totalCarpetArea, totalExistingBuiltUpArea,
	            totalExistingFloorArea, totalExistingCarpetArea, totalBuiltUpArea);

	    updateBuildingTypeFlags(pl, distinctOccupancyTypesHelper);
	}

	
	/**
	 * Sets occupancies and area-related attributes in the {@link Plan} and its virtual building.
	 *
	 * @param pl the {@link Plan} object being processed
	 * @param setOfDistinctOccupancyTypes set of distinct occupancy types in the plan
	 * @param distinctOccupancyTypesHelper helper set of distinct occupancy types
	 * @param totalFloorArea total floor area for all occupancies
	 * @param totalCarpetArea total carpet area for all occupancies
	 * @param totalExistingBuiltUpArea total existing built-up area
	 * @param totalExistingFloorArea total existing floor area
	 * @param totalExistingCarpetArea total existing carpet area
	 * @param totalBuiltUpArea total proposed built-up area
	 */
	private void setOccupanciesAndAreas(Plan pl, Set<OccupancyTypeHelper> setOfDistinctOccupancyTypes,
	        Set<OccupancyTypeHelper> distinctOccupancyTypesHelper, BigDecimal totalFloorArea,
	        BigDecimal totalCarpetArea, BigDecimal totalExistingBuiltUpArea, BigDecimal totalExistingFloorArea,
	        BigDecimal totalExistingCarpetArea, BigDecimal totalBuiltUpArea) {

	    List<Occupancy> occupanciesForPlan = collectOccupanciesForPlan(setOfDistinctOccupancyTypes, pl);
	    pl.setOccupancies(occupanciesForPlan);

	    pl.getVirtualBuilding().setTotalFloorArea(totalFloorArea);
	    pl.getVirtualBuilding().setTotalCarpetArea(totalCarpetArea);
	    pl.getVirtualBuilding().setTotalExistingBuiltUpArea(totalExistingBuiltUpArea);
	    pl.getVirtualBuilding().setTotalExistingFloorArea(totalExistingFloorArea);
	    pl.getVirtualBuilding().setTotalExistingCarpetArea(totalExistingCarpetArea);
	    pl.getVirtualBuilding().setOccupancyTypes(distinctOccupancyTypesHelper);
	    pl.getVirtualBuilding().setTotalBuitUpArea(totalBuiltUpArea);
	    pl.getVirtualBuilding().setMostRestrictiveFarHelper(getMostRestrictiveFar(setOfDistinctOccupancyTypes));
	}

	/**
	 * Updates flags in the virtual building of the plan indicating whether the building is residential or commercial.
	 *
	 * @param pl the {@link Plan} object
	 * @param distinctOccupancyTypesHelper the set of distinct occupancy types used to determine the building type
	 */
	private void updateBuildingTypeFlags(Plan pl, Set<OccupancyTypeHelper> distinctOccupancyTypesHelper) {
	    if (!distinctOccupancyTypesHelper.isEmpty()) {
	        boolean isAllResidential = areAllResidential(distinctOccupancyTypesHelper);
	        pl.getVirtualBuilding().setResidentialBuilding(isAllResidential);

	        boolean isAllResidentialOrCommercial = areAllResidentialOrCommercial(distinctOccupancyTypesHelper);
	        pl.getVirtualBuilding().setResidentialOrCommercialBuilding(isAllResidentialOrCommercial);
	    }
	}

	/**
	 * Checks if all occupancy types are residential.
	 *
	 * @param occupancyTypes set of occupancy types to check
	 * @return {@code true} if all types are residential (type code "A"), otherwise {@code false}
	 */
	private boolean areAllResidential(Set<OccupancyTypeHelper> occupancyTypes) {
	    for (OccupancyTypeHelper occupancy : occupancyTypes) {
	        LOG.info("occupancy :" + occupancy);
	        if (occupancy.getType() == null || !A.equals(occupancy.getType().getCode())) {
	            return false;
	        }
	    }
	    return true;
	}

	/**
	 * Checks if all occupancy types are either residential (code "A") or commercial (code "F").
	 *
	 * @param occupancyTypes set of occupancy types to check
	 * @return {@code true} if all types are residential or commercial, otherwise {@code false}
	 */
	private boolean areAllResidentialOrCommercial(Set<OccupancyTypeHelper> occupancyTypes) {
	    for (OccupancyTypeHelper occupancy : occupancyTypes) {
	        if (occupancy.getType() == null || !(A.equals(occupancy.getType().getCode()) || F.equals(occupancy.getType().getCode()))) {
	            return false;
	        }
	    }
	    return true;
	}


	/**
	 * Aggregates area details (built-up, floor, carpet) across all blocks for a specific occupancy type.
	 *
	 * @param occupancyType the {@link OccupancyTypeHelper} to aggregate for
	 * @param pl the {@link Plan} object containing the blocks and occupancies
	 * @return an {@link Occupancy} object with aggregated values for the specified type
	 */ 	
	private Occupancy aggregateOccupancyForType(OccupancyTypeHelper occupancyType, Plan pl) {
	    BigDecimal totalFloorAreaForAllBlks = BigDecimal.ZERO;
	    BigDecimal totalBuiltUpAreaForAllBlks = BigDecimal.ZERO;
	    BigDecimal totalCarpetAreaForAllBlks = BigDecimal.ZERO;
	    BigDecimal totalExistBuiltUpAreaForAllBlks = BigDecimal.ZERO;
	    BigDecimal totalExistFloorAreaForAllBlks = BigDecimal.ZERO;
	    BigDecimal totalExistCarpetAreaForAllBlks = BigDecimal.ZERO;

	    for (Block block : pl.getBlocks()) {
	        for (Occupancy buildingOccupancy : block.getBuilding().getOccupancies()) {
	            if (occupancyType.equals(buildingOccupancy.getTypeHelper())) {
	                totalFloorAreaForAllBlks = totalFloorAreaForAllBlks.add(buildingOccupancy.getFloorArea());
	                totalBuiltUpAreaForAllBlks = totalBuiltUpAreaForAllBlks.add(buildingOccupancy.getBuiltUpArea());
	                totalCarpetAreaForAllBlks = totalCarpetAreaForAllBlks.add(buildingOccupancy.getCarpetArea());
	                totalExistBuiltUpAreaForAllBlks = totalExistBuiltUpAreaForAllBlks.add(buildingOccupancy.getExistingBuiltUpArea());
	                totalExistFloorAreaForAllBlks = totalExistFloorAreaForAllBlks.add(buildingOccupancy.getExistingFloorArea());
	                totalExistCarpetAreaForAllBlks = totalExistCarpetAreaForAllBlks.add(buildingOccupancy.getExistingCarpetArea());
	            }
	        }
	    }

	    Occupancy occupancy = new Occupancy();
	    occupancy.setTypeHelper(occupancyType);
	    occupancy.setBuiltUpArea(totalBuiltUpAreaForAllBlks);
	    occupancy.setCarpetArea(totalCarpetAreaForAllBlks);
	    occupancy.setFloorArea(totalFloorAreaForAllBlks);
	    occupancy.setExistingBuiltUpArea(totalExistBuiltUpAreaForAllBlks);
	    occupancy.setExistingFloorArea(totalExistFloorAreaForAllBlks);
	    occupancy.setExistingCarpetArea(totalExistCarpetAreaForAllBlks);
	    return occupancy;
	}


	/**
	 * Determines if NOC (No Objection Certificate) is required for the plan based on height, coverage,
	 * or proximity to a monument, and updates the plan information accordingly.
	 *
	 * Sets:
	 * - NOC from Fire Department if building height > 5m or coverage > 500 sq.m.
	 * - NOC near Monument if distance from monument > 300m.
	 *
	 * @param pl the {@link Plan} object to evaluate
	 */
	private void decideNocIsRequired(Plan pl) {
		Boolean isHighRise = false;
		for (Block b : pl.getBlocks()) {
			if ((b.getBuilding() != null && b.getBuilding().getBuildingHeight() != null
					&& b.getBuilding().getBuildingHeight().compareTo(new BigDecimal(5)) > 0)
					|| (b.getBuilding() != null && b.getBuilding().getCoverageArea() != null
							&& b.getBuilding().getCoverageArea().compareTo(new BigDecimal(500)) > 0)) {
				isHighRise = true;

			}
		}
		if (isHighRise) {
			pl.getPlanInformation().setNocFireDept(DcrConstants.YES);
		}

		if (StringUtils.isNotBlank(pl.getPlanInformation().getBuildingNearMonument())
				&& DcrConstants.YES.equalsIgnoreCase(pl.getPlanInformation().getBuildingNearMonument())) {
			BigDecimal minDistanceFromMonument = BigDecimal.ZERO;
			List<BigDecimal> distancesFromMonument = pl.getDistanceToExternalEntity().getMonuments();
			if (!distancesFromMonument.isEmpty()) {

				minDistanceFromMonument = distancesFromMonument.stream().reduce(BigDecimal::min).get();

				if (minDistanceFromMonument.compareTo(BigDecimal.valueOf(300)) > 0) {
					pl.getPlanInformation().setNocNearMonument(DcrConstants.YES);
				}
			}

		}

	}

	/**
	 * Validates occupancy data for a specific block and floor, checking for negative built-up, floor, or existing areas.
	 * Adds validation errors to the plan if any area values are negative.
	 *
	 * @param pl the {@link Plan} to record errors in
	 * @param blk the {@link Block} being validated
	 * @param flr the {@link Floor} under validation
	 * @param occupancy the {@link Occupancy} object being checked
	 */
	private void validate2(Plan pl, Block blk, Floor flr, Occupancy occupancy) {
		String occupancyTypeHelper = StringUtils.EMPTY;
		if (occupancy.getTypeHelper() != null) {
			if (occupancy.getTypeHelper().getType() != null) {
				occupancyTypeHelper = occupancy.getTypeHelper().getType().getName();
			} else if (occupancy.getTypeHelper().getSubtype() != null) {
				occupancyTypeHelper = occupancy.getTypeHelper().getSubtype().getName();
			}
		}

		if (occupancy.getBuiltUpArea() != null && occupancy.getBuiltUpArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_BUILTUP_AREA, getLocaleMessage(VALIDATION_NEGATIVE_BUILTUP_AREA,
					blk.getNumber(), flr.getNumber().toString(), occupancyTypeHelper));
		}
		if (occupancy.getExistingBuiltUpArea() != null
				&& occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_EXISTING_BUILTUP_AREA,
					getLocaleMessage(VALIDATION_NEGATIVE_EXISTING_BUILTUP_AREA, blk.getNumber(),
							flr.getNumber().toString(), occupancyTypeHelper));
		}
		occupancy.setFloorArea((occupancy.getBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getBuiltUpArea())
				.subtract(occupancy.getDeduction() == null ? BigDecimal.ZERO : occupancy.getDeduction()));
		if (occupancy.getFloorArea() != null && occupancy.getFloorArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_FLOOR_AREA, getLocaleMessage(VALIDATION_NEGATIVE_FLOOR_AREA,
					blk.getNumber(), flr.getNumber().toString(), occupancyTypeHelper));
		}
		occupancy.setExistingFloorArea(
				(occupancy.getExistingBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getExistingBuiltUpArea())
						.subtract(occupancy.getExistingDeduction() == null ? BigDecimal.ZERO
								: occupancy.getExistingDeduction()));
		if (occupancy.getExistingFloorArea() != null
				&& occupancy.getExistingFloorArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_EXISTING_FLOOR_AREA,
					getLocaleMessage(VALIDATION_NEGATIVE_EXISTING_FLOOR_AREA, blk.getNumber(),
							flr.getNumber().toString(), occupancyTypeHelper));
		}
	}

	/**
	 * Identifies and returns the most restrictive FAR (Floor Area Ratio) type among the given occupancy types
	 * based on a predefined order of precedence.
	 *
	 * @param distinctOccupancyTypes set of distinct occupancy types to evaluate
	 * @return the {@link OccupancyTypeHelper} with the most restrictive FAR requirement
	 */
	protected OccupancyTypeHelper getMostRestrictiveFar(Set<OccupancyTypeHelper> distinctOccupancyTypes) {
		Set<String> codes = new HashSet<>();
		Map<String, OccupancyTypeHelper> codesMap = new HashMap<>();
		for (OccupancyTypeHelper typeHelper : distinctOccupancyTypes) {

			if (typeHelper.getType() != null)
				codesMap.put(typeHelper.getType().getCode(), typeHelper);
			if (typeHelper.getSubtype() != null)
				codesMap.put(typeHelper.getSubtype().getCode(), typeHelper);
		}
		codes = codesMap.keySet();
		if (codes.contains(S_ECFG))
			return codesMap.get(S_ECFG);
		else if (codes.contains(A_FH))
			return codesMap.get(A_FH);
		else if (codes.contains(S_SAS))
			return codesMap.get(S_SAS);
		else if (codes.contains(D_B))
			return codesMap.get(D_B);
		else if (codes.contains(D_C))
			return codesMap.get(D_C);
		else if (codes.contains(D_A))
			return codesMap.get(D_A);
		else if (codes.contains(H_PP))
			return codesMap.get(H_PP);
		else if (codes.contains(E_NS))
			return codesMap.get(E_NS);
		else if (codes.contains(M_DFPAB))
			return codesMap.get(M_DFPAB);
		else if (codes.contains(E_PS))
			return codesMap.get(E_PS);
		else if (codes.contains(E_SFMC))
			return codesMap.get(E_SFMC);
		else if (codes.contains(E_SFDAP))
			return codesMap.get(E_SFDAP);
		else if (codes.contains(E_EARC))
			return codesMap.get(E_EARC);
		else if (codes.contains(S_MCH))
			return codesMap.get(S_MCH);
		else if (codes.contains(S_BH))
			return codesMap.get(S_BH);
		else if (codes.contains(S_CRC))
			return codesMap.get(S_CRC);
		else if (codes.contains(S_CA))
			return codesMap.get(S_CA);
		else if (codes.contains(S_SC))
			return codesMap.get(S_SC);
		else if (codes.contains(S_ICC))
			return codesMap.get(S_ICC);
		else if (codes.contains(A2))
			return codesMap.get(A2);
		else if (codes.contains(E_CLG))
			return codesMap.get(E_CLG);
		else if (codes.contains(M_OHF))
			return codesMap.get(M_OHF);
		else if (codes.contains(M_VH))
			return codesMap.get(M_VH);
		else if (codes.contains(M_NAPI))
			return codesMap.get(M_NAPI);
		else if (codes.contains(A_SA))
			return codesMap.get(A_SA);
		else if (codes.contains(M_HOTHC))
			return codesMap.get(M_HOTHC);
		else if (codes.contains(E_SACA))
			return codesMap.get(E_SACA);
		else if (codes.contains(G))
			return codesMap.get(G);
		else if (codes.contains(F))
			return codesMap.get(F);
		else if (codes.contains(A))
			return codesMap.get(A);
		else
			return null;

	}

	private Boolean processFarForSpecialOccupancy(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far,
			String typeOfArea, BigDecimal roadWidth, HashMap<String, String> errors) {

		OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;
		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;
		if (mostRestrictiveOccupancyType != null && mostRestrictiveOccupancyType.getSubtype() != null) {
			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_ECFG)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(A_FH)) {
				isAccepted = far.compareTo(POINTTWO) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ZERO_POINT_TWO;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_SAS)) {
				isAccepted = far.compareTo(POINTFOUR) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ZERO_POINT_FOUR;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(D_B)) {
				isAccepted = far.compareTo(POINTFIVE) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ZERO_POINT_FIVE;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(D_C)) {
				isAccepted = far.compareTo(POINTSIX) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ZERO_POINT_SIX;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(D_A)) {
				isAccepted = far.compareTo(POINTSEVEN) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ZERO_POINT_SEVEN;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(H_PP)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_NS)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_DFPAB)) {
				isAccepted = far.compareTo(ONE) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ONE;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_PS)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_SFMC)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_SFDAP)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_EARC)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_MCH)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_BH)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_CRC)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_CA)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_SC)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_ICC)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(A2)) {
				isAccepted = far.compareTo(ONE_POINTTWO) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ONE_POINT_TWO;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(B2)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_CLG)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_OHF)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_VH)
					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_NAPI)) {
				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_ONE_POINT_FIVE;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(A_SA)) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_TWO_POINT_FIVE;
				return true;
			}

			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_SACA)) {
				isAccepted = far.compareTo(FIFTEEN) <= 0;
				expectedResult = LESS_THAN_EQUAL_TO_FIFTEEN;
				return true;
			}

		}

		String occupancyName = occupancyType.getSubtype() != null ? occupancyType.getSubtype().getName()
				: occupancyType.getType().getName();

		if (StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}

		return false;
	}

	/**
	 * Processes and validates the Floor Area Ratio (FAR) for residential occupancy
	 * based on plot area, road width, and permissible FAR rules fetched from the
	 * cache.
	 *
	 * <p>
	 * This method:
	 * <ul>
	 * <li>Fetches applicable FAR rules for residential occupancy from the MDMS
	 * feature rule cache.</li>
	 * <li>Determines the permissible FAR by matching the plot area range with the
	 * rules.</li>
	 * <li>Compares the actual FAR with the permissible FAR to check if it's
	 * compliant.</li>
	 * <li>Updates the {@link Plan} with the permissible FAR and builds the result
	 * if valid.</li>
	 * </ul>
	 *
	 * @param pl            the {@link Plan} object containing details of the plot
	 *                      and FAR to validate
	 * @param occupancyType the {@link OccupancyTypeHelper} representing the current
	 *                      occupancy type
	 * @param far           the actual FAR value for the given occupancy
	 * @param typeOfArea    a string indicating the type of area being evaluated
	 *                      (e.g., "Built-up", "Floor", etc.)
	 * @param roadWidth     the width of the road adjacent to the plot
	 * @param errors        a map of validation errors, to which any issues found
	 *                      during validation may be added
	 * @param feature       the feature name (e.g., FAR) used to query rules from
	 *                      the cache
	 * @param occupancyName the name of the occupancy type being evaluated (e.g.,
	 *                      "Residential")
	 */

	private void processFar(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
			BigDecimal roadWidth, HashMap<String, String> errors, String feature, String occupancyName) {

		final BigDecimal ONE_BIGHA_IN_SQM = ONEBIGHA;
		BigDecimal plotArea = pl.getPlot().getArea();
		BigDecimal permissibleFar = BigDecimal.ZERO;

		LOG.debug("Starting processFar with plotArea: {}, far: {}, roadWidth: {}", plotArea, far, roadWidth);

		OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;

		LOG.debug("Most restrictive occupancy type: {}", mostRestrictiveOccupancyType);

		Optional<FarRequirement> matchedRule = findMatchedFarRule(pl, mostRestrictiveOccupancyType, plotArea,
				roadWidth);

		if (matchedRule.isPresent()) {
			permissibleFar = matchedRule.get().getPermissible();
			LOG.info("Permissible FAR from matched rule: {}", permissibleFar);

// Apply 30% Mixed Use FAR only if it's A_AF (Apartment) and plot area < 1 bigha
			if (isResidentialApartmentEligibleForMixedUse(occupancyType, plotArea, ONE_BIGHA_IN_SQM)) {
				permissibleFar = applyMixedUseFARIfApplicable(pl, occupancyType, permissibleFar);
				LOG.info("Mixed-use FAR applied. Updated permissible FAR: {}", permissibleFar);
			}

// Apply 25% EWS/LIG FAR increase only for Group Housing
			if (isGroupHousingWithEWSLIG(pl, occupancyType, plotArea)) {
				permissibleFar = applyEWSLIGFarRelaxationIfApplicable(permissibleFar);
				LOG.info("25% additional FAR applied for Group Housing with EWS/LIG. New permissible FAR: {}",
						permissibleFar);
			}

		} else {
			LOG.warn("No FAR rule matched for given parameters: plotArea={}, roadWidth={}", plotArea, roadWidth);
		}

		try {
			LOG.info("Final permissible FAR to validate against: {}", permissibleFar);
		} catch (NullPointerException e) {
			LOG.error("Permissible FAR not found or null", e);
		}

		boolean isAccepted = far.compareTo(permissibleFar) <= 0;
		pl.getFarDetails().setPermissableFar(permissibleFar.doubleValue());
		String expectedResult = "<= " + permissibleFar;

		LOG.debug("FAR validation result for occupancy '{}': provided FAR = {}, accepted = {}", occupancyName, far,
				isAccepted);

		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private Optional<FarRequirement> findMatchedFarRule(Plan pl, OccupancyTypeHelper occupancy, BigDecimal plotArea,
			BigDecimal roadWidth) {
		LOG.debug("Finding matched FAR rule with plotArea: {}, roadWidth: {}", plotArea, roadWidth);

		List<Object> rules = cache.getFeatureRules(pl, FeatureEnum.FAR.getValue(), false);

		if (occupancy == null || occupancy.getType() == null) {
			LOG.warn("Occupancy or occupancy type is null, cannot find matched FAR rule.");
			return Optional.empty();
		}

		String occCode = occupancy.getType().getCode();
		LOG.debug("Occupancy code for FAR matching: {}", occCode);

		if (B.equalsIgnoreCase(occCode) || H.equalsIgnoreCase(occCode) || D.equalsIgnoreCase(occCode)) {
			LOG.debug("Matching FAR based on road width for industrial or similar occupancy.");
			return rules.stream().filter(FarRequirement.class::isInstance).map(FarRequirement.class::cast)
					.filter(rule -> roadWidth.compareTo(rule.getFromRoadWidth()) >= 0
							&& roadWidth.compareTo(rule.getToRoadWidth()) < 0)
					.findFirst();

		} else if (J.equalsIgnoreCase(occCode)) {
			LOG.debug("Matching FAR based on plot area only for special occupancy.");
			return rules.stream().filter(FarRequirement.class::isInstance).map(FarRequirement.class::cast)
					.filter(rule -> plotArea.compareTo(rule.getFromPlotArea()) >= 0
							&& plotArea.compareTo(rule.getToPlotArea()) < 0)
					.findFirst();

		} else {
			LOG.debug("Matching FAR based on plot area and road width for default occupancy.");
			return rules.stream().filter(FarRequirement.class::isInstance).map(FarRequirement.class::cast)
					.filter(rule -> plotArea.compareTo(rule.getFromPlotArea()) >= 0
							&& plotArea.compareTo(rule.getToPlotArea()) < 0
							&& roadWidth.compareTo(rule.getFromRoadWidth()) >= 0
							&& roadWidth.compareTo(rule.getToRoadWidth()) < 0)
					.findFirst();
		}
	}

private boolean isResidentialApartmentEligibleForMixedUse(OccupancyTypeHelper occupancyType, BigDecimal plotArea,
		BigDecimal oneBigha) {
	boolean eligible = occupancyType != null && occupancyType.getSubtype() != null
			&& A_AF.equalsIgnoreCase(occupancyType.getSubtype().getCode()) && plotArea.compareTo(oneBigha) < 0;
	LOG.debug("Residential apartment eligible for mixed use: {}", eligible);
	return eligible;
}

private void processFarIndustrial(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
		BigDecimal roadWidth, HashMap<String, String> errors, String feature, String occupancyName) {

	BigDecimal permissibleFar = BigDecimal.ZERO;

	OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding() != null
			? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
			: null;

	String subtypeCode = mostRestrictiveOccupancyType != null && mostRestrictiveOccupancyType.getSubtype() != null
			? mostRestrictiveOccupancyType.getSubtype().getCode()
			: null;

	LOG.debug("Processing FAR for industrial occupancy, subtype: {}", subtypeCode);

	List<Object> rules = cache.getFeatureRules(pl, FeatureEnum.FAR.getValue(), false);
	Optional<FarRequirement> matchedRule = rules.stream().filter(FarRequirement.class::isInstance)
			.map(FarRequirement.class::cast).filter(ruleObj -> Boolean.TRUE.equals(ruleObj.getActive())).findFirst();

	if (matchedRule.isPresent()) {
		FarRequirement mdmsRule = matchedRule.get();

		if (G_SI.equalsIgnoreCase(subtypeCode)) {
			permissibleFar = mdmsRule.getPermissibleLight();
		} else if (G_LI.equalsIgnoreCase(subtypeCode)) {
			permissibleFar = mdmsRule.getPermissibleMedium();
		} else if (G_PHI.equalsIgnoreCase(subtypeCode)) {
			permissibleFar = mdmsRule.getPermissibleFlattered();
		} else {
			permissibleFar = mdmsRule.getPermissible(); // fallback
		}
		LOG.info("Permissible FAR for industrial subtype '{}': {}", subtypeCode, permissibleFar);
	} else {
		LOG.warn("No active FAR rule found for industrial processing.");
	}

	boolean isAccepted = far.compareTo(permissibleFar) <= 0;
	pl.getFarDetails().setPermissableFar(permissibleFar.doubleValue());
	String expectedResult = LESS_THAN_EQUAL_TO + permissibleFar;

	LOG.debug("Industrial FAR validation result for occupancy '{}': provided FAR = {}, accepted = {}", occupancyName,
			far, isAccepted);

	if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
		buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
	}
}

private BigDecimal applyMixedUseFARIfApplicable(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal permissibleFar) {

	BigDecimal additionalMixedUseFar = permissibleFar.multiply(POINTTHREE);
	permissibleFar = permissibleFar.add(additionalMixedUseFar);

	LOG.info("30% mixed-use FAR applied for residential plot < 1 bigha. New Permissible FAR = {}", permissibleFar);
	return permissibleFar;
}

private boolean isGroupHousingWithEWSLIG(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal plotArea) {
	boolean result = occupancyType != null && occupancyType.getSubtype() != null
			&& A_AF_GH.equalsIgnoreCase(occupancyType.getSubtype().getCode()) && plotArea.compareTo(TWOTHOUSAND) >= 0
			&& pl.getPlanInformation() != null && pl.getPlanInformation().getPlotType() != null
			&& (pl.getPlanInformation().getPlotType().equalsIgnoreCase(EWS)
					|| pl.getPlanInformation().getPlotType().equalsIgnoreCase(LIG));
	LOG.debug("Group housing with EWS/LIG eligibility: {}", result);
	return result;
}

private BigDecimal applyEWSLIGFarRelaxationIfApplicable(BigDecimal permissibleFar) {
	BigDecimal additionalFar = permissibleFar.multiply(POINTTWOFIVE);
	BigDecimal relaxedFar = permissibleFar.add(additionalFar);
	LOG.info("Applied 25% EWS/LIG FAR relaxation. New permissible FAR: {}", relaxedFar);
	return relaxedFar;
}

	private void processFarNonResidential(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
			BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;

		if (typeOfArea.equalsIgnoreCase(OLD)) {
			if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOUR) < 0) {
				errors.put(OLD_AREA_ERROR, OLD_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOURFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_THREE_POINTSIX) < 0) {
				isAccepted = far.compareTo(ONE_POINTTWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_ONE_POINT_TWO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_THREE_POINTSIX) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_FOUR_POINTEIGHT) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = INT_ZERO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_FOUR_POINTEIGHT) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = INT_ZERO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = INT_ZERO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = INT_ZERO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) < 0) {
				isAccepted = far.compareTo(TWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_TWO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) >= 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_TWO_POINT_FIVE;
			}

		}

		if (typeOfArea.equalsIgnoreCase(NEW)) {
			if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				errors.put(NEW_AREA_ERROR, NEW_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = INT_ZERO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = INT_ZERO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) < 0) {
				isAccepted = far.compareTo(TWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_TWO;
			} else if (roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_TWO_POINT_FIVE;
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWENTYSEVEN_POINTFOUR) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_TWO_POINT_FIVE;
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYSEVEN_POINTFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_THIRTY_POINTFIVE) < 0) {
				isAccepted = far.compareTo(THREE) <= 0;
				pl.getFarDetails().setPermissableFar(THREE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_THREE;
			} else if (roadWidth.compareTo(ROAD_WIDTH_THIRTY_POINTFIVE) >= 0) {
				isAccepted = far.compareTo(THREE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(THREE_POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO_THREE;
			}

		}

		String occupancyName = occupancyType.getType().getName();

		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private void processFarForGBDOccupancy(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far,
			String typeOfArea, BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;

		if (typeOfArea.equalsIgnoreCase(OLD)) {
			if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOUR) < 0) {
				errors.put(OLD_AREA_ERROR, OLD_AREA_ERROR_MSG);
				pl.addErrors(errors);
				return;
			} else {
				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO + ONE_POINTFIVE;
			}

		}

		if (typeOfArea.equalsIgnoreCase(NEW)) {
			if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				errors.put(NEW_AREA_ERROR, NEW_AREA_ERROR_MSG);
				pl.addErrors(errors);
				return;
			} else {
				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO + ONE_POINTFIVE;
			}

		}

		String occupancyName = occupancyType.getType().getName();

		if (occupancyType.getSubtype() != null) {
			OccupancyHelperDetail subtype = occupancyType.getSubtype();
			occupancyName = subtype.getName();
			String code = subtype.getCode();

			if (G_PHI.equalsIgnoreCase(code)) {
				isAccepted = far.compareTo(POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO + POINTFIVE;
			} else if (G_NPHI.equalsIgnoreCase(code)) {
				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO + ONE_POINTFIVE;
			}
		}

		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private void processFarHaazardous(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
			BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;

		if (typeOfArea.equalsIgnoreCase(OLD)) {
			if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOUR) < 0) {
				errors.put(OLD_AREA_ERROR, OLD_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else {
				isAccepted = far.compareTo(POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO + POINTFIVE;
			}

		}

		if (typeOfArea.equalsIgnoreCase(NEW)) {
			if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				errors.put(NEW_AREA_ERROR, NEW_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else {
				isAccepted = far.compareTo(POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(POINTFIVE.doubleValue());
				expectedResult = LESS_THAN_EQUAL_TO + POINTFIVE;
			}

		}

		String occupancyName = occupancyType.getType().getName();

		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private void buildResult(Plan pl, String occupancyName, BigDecimal far, String typeOfArea, BigDecimal roadWidth,
			String expectedResult, boolean isAccepted) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, OCCUPANCY);
		scrutinyDetail.addColumnHeading(3, AREA_TYPE);
		scrutinyDetail.addColumnHeading(4, ROAD_WIDTH);
		scrutinyDetail.addColumnHeading(5, PERMISSIBLE);
		scrutinyDetail.addColumnHeading(6, PROVIDED);
		scrutinyDetail.addColumnHeading(7, STATUS);
		scrutinyDetail.setKey(COMMON_FAR);

		String actualResult = far.toString();

		ReportScrutinyDetail detail = new ReportScrutinyDetail();
		detail.setRuleNo(RULE_38);
		detail.setOccupancy(occupancyName);
		detail.setAreaType(typeOfArea);
		detail.setRoadWidth(roadWidth.toString());
		detail.setPermissible(expectedResult);
		detail.setProvided(actualResult);
		detail.setStatus(isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

		Map<String, String> details = mapReportDetails(detail);
		addScrutinyDetailtoPlan(scrutinyDetail, pl, details);
	}

	private ScrutinyDetail getFarScrutinyDetail(String key) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, AREA_TYPE);
		scrutinyDetail.addColumnHeading(3, ROAD_WIDTH);
		scrutinyDetail.addColumnHeading(4, PERMISSIBLE);
		scrutinyDetail.addColumnHeading(5, PROVIDED);
		scrutinyDetail.addColumnHeading(6, STATUS);
		scrutinyDetail.setKey(key);
		return scrutinyDetail;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}
