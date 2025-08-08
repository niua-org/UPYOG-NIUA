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

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.entity.edcr.*;
import org.egov.edcr.service.MDMSCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.egov.edcr.constants.EdcrReportConstants.*;
import static org.egov.edcr.service.FeatureUtil.mapReportDetails;

@Service
public class BathRoom_Assam extends BathRoom {

    // Logger for logging information and errors
    private static final Logger LOG = LogManager.getLogger(BathRoom_Assam.class);

    @Autowired
	MDMSCacheManager cache;

    @Override
    public Plan validate(Plan pl) {
        // Currently, no validation logic is implemented
        return pl;
    }
    
    /**
     * Processes the given {@link Plan} object to validate bathroom dimensions (area and width) on each floor of all blocks,
     * based on the rules retrieved from MDMS configuration. Adds scrutiny details to the report if validations are performed.
     *
     * @param pl The plan to be processed.
     * @return The processed plan with scrutiny details updated.
     */
    
    @Override
    public Plan process(Plan pl) {
       

        ScrutinyDetail scrutinyDetail = createScrutinyDetail();

        if (pl.getBlocks() == null || pl.getBlocks().isEmpty()) {
          
            return pl;
        }

        for (Block block : pl.getBlocks()) {
            LOG.debug("Processing Block ID: {}", block.getNumber());
            processBlock(pl, block, scrutinyDetail);
        }

        if (!scrutinyDetail.getDetail().isEmpty()) {
           
            pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
        } 

      
        return pl;
    }

    /**
     * Processes an individual block within the plan to validate bathroom rules.
     */
    private void processBlock(Plan plan, Block block, ScrutinyDetail scrutinyDetail) {
        if (block.getBuilding() == null || block.getBuilding().getFloors() == null) {
            LOG.debug("Skipping Block ID: {} due to missing building or floors", block.getNumber());
            return;
        }

        List<Object> rules = cache.getFeatureRules(plan, FeatureEnum.BATHROOM.getValue(), false);
        Optional<BathroomRequirement> matchedRule = rules.stream()
            .filter(BathroomRequirement.class::isInstance)
            .map(BathroomRequirement.class::cast)
            .findFirst();

        if (!matchedRule.isPresent()) {
            LOG.warn("No bathroom rules found in cache for Bathroom");
            return;
        }

        BathroomRequirement rule = matchedRule.get();

        BigDecimal permittedArea = rule.getBathroomtotalArea() != null ? rule.getBathroomtotalArea() : BigDecimal.ZERO;
        BigDecimal permittedMinWidth = rule.getBathroomMinWidth() != null ? rule.getBathroomMinWidth() : BigDecimal.ZERO;

        LOG.debug("Bathroom rules fetched - Permitted Area: {}, Permitted Min Width: {} for Block ID: {}",
                permittedArea, permittedMinWidth, block.getNumber());

        for (Floor floor : block.getBuilding().getFloors()) {
            LOG.debug("Processing Floor Number: {} in Block ID: {}", floor.getNumber(), block.getNumber());
            processFloor(plan, floor, permittedArea, permittedMinWidth, scrutinyDetail);
        }
    }

    /**
     * Processes an individual floor to extract bathroom measurements and perform validations.
     */
    private void processFloor(Plan plan, Floor floor, BigDecimal permittedArea, BigDecimal permittedMinWidth,
                              ScrutinyDetail scrutinyDetail) {
        Room bathRoom = floor.getBathRoom();
        if (bathRoom == null || bathRoom.getRooms() == null || bathRoom.getHeights() == null) {
            LOG.debug("Skipping Floor Number: {} due to missing bathroom details", floor.getNumber());
            return;
        }

        List<Measurement> rooms = bathRoom.getRooms();
        List<RoomHeight> heights = bathRoom.getHeights();

        if (rooms.isEmpty() || heights.isEmpty()) {
            LOG.debug("Skipping Floor Number: {} due to empty room or height details", floor.getNumber());
            return;
        }

        validateBathroom(plan, floor, rooms, heights, permittedArea, permittedMinWidth, scrutinyDetail);
        validateBathroomVentilation(floor, scrutinyDetail, plan);
    }

    /**
     * Validates the area, width, and height of bathroom rooms on a floor against the permitted values.
     */
    private void validateBathroom(Plan plan, Floor floor, List<Measurement> rooms, List<RoomHeight> heights,
                                  BigDecimal permittedArea, BigDecimal permittedMinWidth, ScrutinyDetail scrutinyDetail) {

        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal minWidth = rooms.get(0).getWidth();

        for (Measurement m : rooms) {
            totalArea = totalArea.add(m.getArea());
            if (m.getWidth().compareTo(minWidth) < 0) {
                minWidth = m.getWidth();
            }
        }

        BigDecimal minHeight = heights.get(0).getHeight();
        for (RoomHeight rh : heights) {
            if (rh.getHeight().compareTo(minHeight) < 0) {
                minHeight = rh.getHeight();
            }
        }

        boolean isAccepted = totalArea.compareTo(permittedArea) >= 0 && minWidth.compareTo(permittedMinWidth) >= 0;

        LOG.debug("Validating bathroom for Floor Number: {} - Total Area: {}, Min Width: {}, Min Height: {}, " +
                  "Permitted Area: {}, Permitted Min Width: {}, Accepted: {}",
                floor.getNumber(), totalArea, minWidth, minHeight, permittedArea, permittedMinWidth, isAccepted);

        Map<String, String> resultRow = createResultRow(floor, permittedArea, permittedMinWidth, totalArea, minWidth, isAccepted);
        scrutinyDetail.getDetail().add(resultRow);
    }

    private void validateBathroomVentilation(Floor floor, ScrutinyDetail scrutinyDetail, Plan pl) {
        if (floor.getBathRoom() == null ||
            floor.getBathRoom().getBathVentilation() == null ||
            floor.getBathRoom().getBathVentilation().isEmpty()) {

            LOG.warn("Bathroom ventilation measurements missing on Floor Number: {}", floor.getNumber());

            ReportScrutinyDetail detail = new ReportScrutinyDetail();
            detail.setRuleNo("91 d");
            detail.setDescription("Bathroom - Ventilation Area");
            detail.setRequired("Not defined");
            detail.setProvided("Bath ventilation measurements not available on floor " + floor.getNumber());
            detail.setStatus(Result.Not_Accepted.getResultVal());

            scrutinyDetail.getDetail().add(mapReportDetails(detail));
            return;
        }

        List<Object> rules = cache.getFeatureRules(pl, FeatureEnum.BATHROOM.getValue(), false);
        Optional<BathroomRequirement> matchedRule = rules.stream()
            .filter(BathroomRequirement.class::isInstance)
            .map(BathroomRequirement.class::cast)
            .findFirst();

        BigDecimal requiredArea = BigDecimal.ZERO;
        BigDecimal requiredWidth = BigDecimal.ZERO;

        if (matchedRule.isPresent()) {
            BathroomRequirement rule = matchedRule.get();
            requiredArea = rule.getBathAndStoreVentilationArea();
            requiredWidth = rule.getBathAndStoreVentilationWidth();

            LOG.debug("Bathroom ventilation rules fetched - Required Area: {}, Required Width: {} for Floor Number: {}",
                    requiredArea, requiredWidth, floor.getNumber());
        } else {
            LOG.warn("No bathroom ventilation rules found in cache");
        }

        List<Measurement> bathVentilation = floor.getBathRoom().getBathVentilation();

        BigDecimal providedArea = bathVentilation.stream()
            .map(Measurement::getArea)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal providedWidth = bathVentilation.stream()
            .map(Measurement::getWidth)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (requiredArea.compareTo(BigDecimal.ZERO) > 0) {
            boolean acceptedArea = providedArea.compareTo(requiredArea) >= 0;
            LOG.debug("Bathroom ventilation area validation for Floor Number: {} Provided: {}, Required: {}, Accepted: {}",
                    floor.getNumber(), providedArea, requiredArea, acceptedArea);

            ReportScrutinyDetail detail = new ReportScrutinyDetail();
            detail.setRuleNo("91 d");
            detail.setDescription("Bathroom - Ventilation Area");
            detail.setRequired(requiredArea + " sqm");
            detail.setProvided(providedArea + " sqm at floor " + floor.getNumber());
            detail.setStatus(acceptedArea ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

            scrutinyDetail.getDetail().add(mapReportDetails(detail));
        }

        if (requiredWidth.compareTo(BigDecimal.ZERO) > 0) {
            boolean acceptedWidth = providedWidth.compareTo(requiredWidth) >= 0;
            LOG.debug("Bathroom ventilation width validation for Floor Number: {} Provided: {}, Required: {}, Accepted: {}",
                    floor.getNumber(), providedWidth, requiredWidth, acceptedWidth);

            ReportScrutinyDetail detail = new ReportScrutinyDetail();
            detail.setRuleNo("91 d");
            detail.setDescription("Bathroom - Ventilation Width");
            detail.setRequired(requiredWidth + " m");
            detail.setProvided(providedWidth + " m at floor " + floor.getNumber());
            detail.setStatus(acceptedWidth ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

            scrutinyDetail.getDetail().add(mapReportDetails(detail));
        }
    }

    /**
     * Creates a new {@link ScrutinyDetail} object and initializes it with column headings.
     */
    private ScrutinyDetail createScrutinyDetail() {
        LOG.debug("Creating ScrutinyDetail object for Bathroom rules");
        ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
        scrutinyDetail.setKey(Common_Bathroom);
        scrutinyDetail.addColumnHeading(1, RULE_NO);
        scrutinyDetail.addColumnHeading(2, DESCRIPTION);
        scrutinyDetail.addColumnHeading(3, REQUIRED);
        scrutinyDetail.addColumnHeading(4, PROVIDED);
        scrutinyDetail.addColumnHeading(5, STATUS);
        return scrutinyDetail;
    }

    /**
     * Creates a map representing a single row of bathroom validation results for a floor.
     */
    private Map<String, String> createResultRow(Floor floor, BigDecimal permittedArea, BigDecimal permittedMinWidth,
                                                BigDecimal totalArea, BigDecimal minWidth, boolean isAccepted) {
        ReportScrutinyDetail detail = new ReportScrutinyDetail();
        detail.setRuleNo(RULE_41_IV);
        detail.setDescription(BATHROOM_DESCRIPTION);
        detail.setRequired(TOTAL_AREA + permittedArea.toString() + WIDTH + permittedMinWidth.toString());
        detail.setProvided(TOTAL_AREA + totalArea.toString() + WIDTH + minWidth.toString());
        detail.setStatus(isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

        LOG.debug("Created result row for Floor Number: {} with acceptance: {}", floor.getNumber(), isAccepted);

        return mapReportDetails(detail);
    }

    /**
     * Retrieves a new Amendment object instance.
     */
    @Override
    public Map<String, Date> getAmendments() {
        LOG.debug("Retrieving amendments - returning empty map");
        return new LinkedHashMap<>();
    }
}
