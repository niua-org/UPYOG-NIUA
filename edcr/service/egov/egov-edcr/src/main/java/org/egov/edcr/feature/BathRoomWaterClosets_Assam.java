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
public class BathRoomWaterClosets_Assam extends BathRoomWaterClosets {

    // Logger for logging information and errors
    private static final Logger LOG = LogManager.getLogger(BathRoomWaterClosets_Assam.class);

    /**
     * This method is used to validate the plan object.
     * Currently, no validation logic is implemented.
     *
     * @param pl The plan object to validate.
     * @return The same plan object without any modifications.
     */
    @Override
    public Plan validate(Plan pl) {
        return pl;
    }


    @Autowired
   	MDMSCacheManager cache;

    /**
     * Processes the given {@link Plan} to validate bathroom water closet areas, widths, and heights
     * based on feature rules defined in MDMS.
     *
     * @param pl the plan to process
     * @return the processed plan with scrutiny details added if validation results are present
     */

    @Override
    public Plan process(Plan pl) {
        LOG.info("Starting bathroom water closet processing");

        ScrutinyDetail scrutinyDetail = createScrutinyDetail();

        List<Object> rules = cache.getFeatureRules(pl, FeatureEnum.BATHROOM_WATER_CLOSETS.getValue(), false);
        Optional<BathroomWCRequirement> matchedRule = rules.stream()
            .filter(BathroomWCRequirement.class::isInstance)
            .map(BathroomWCRequirement.class::cast)
            .findFirst();

        if (!matchedRule.isPresent()) {
            LOG.warn("No bathroom water closet rules found, skipping processing");
            return pl;
        }

        BathroomWCRequirement rule = matchedRule.get();
        BigDecimal reqArea = rule.getBathroomWCRequiredArea();
        BigDecimal reqWidth = rule.getBathroomWCRequiredWidth();
        BigDecimal reqHeight = rule.getBathroomWCRequiredHeight();

        LOG.debug("Bathroom WC rules fetched - Required Area: {}, Required Width: {}, Required Height: {}",
                reqArea, reqWidth, reqHeight);

        if (pl.getBlocks() == null || pl.getBlocks().isEmpty()) {
            LOG.warn("No blocks present in plan, skipping processing");
            return pl;
        }

        for (Block block : pl.getBlocks()) {
            LOG.debug("Processing Block Number: {}", block.getNumber());
            processBlock(pl, block, reqArea, reqWidth, reqHeight, scrutinyDetail);
        }

        if (!scrutinyDetail.getDetail().isEmpty()) {
            LOG.info("Adding scrutiny details to report");
            pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
        } else {
            LOG.info("No scrutiny details generated after processing");
        }

        LOG.info("Completed bathroom water closet processing");
        return pl;
    }

    /**
     * Processes each block in the plan and validates the bathroom water closet
     * dimensions for each floor.
     */
    private void processBlock(Plan plan, Block block, BigDecimal reqArea, BigDecimal reqWidth, BigDecimal reqHeight,
                              ScrutinyDetail scrutinyDetail) {
        if (block.getBuilding() == null || block.getBuilding().getFloors() == null) {
            LOG.debug("Skipping Block Number: {} due to missing building or floors", block.getNumber());
            return;
        }

        for (Floor floor : block.getBuilding().getFloors()) {
            LOG.debug("Processing Floor Number: {} in Block Number: {}", floor.getNumber(), block.getNumber());
            processFloor(plan, floor, reqArea, reqWidth, reqHeight, scrutinyDetail);
        }
    }

    /**
     * Processes each floor of a block and validates bathroom WC measurements.
     */
    private void processFloor(Plan plan, Floor floor, BigDecimal reqArea, BigDecimal reqWidth, BigDecimal reqHeight,
                              ScrutinyDetail scrutinyDetail) {
        org.egov.common.entity.edcr.Room bathWC = floor.getBathRoomWaterClosets();
        if (bathWC == null || bathWC.getHeights() == null || bathWC.getHeights().isEmpty()
            || bathWC.getRooms() == null || bathWC.getRooms().isEmpty()) {
            LOG.debug("Skipping Floor Number: {} due to missing bathroom WC room or height details", floor.getNumber());
            return;
        }

        LOG.debug("Validating bathroom water closet on Floor Number: {}", floor.getNumber());
        validateBathroomWaterCloset(plan, floor, bathWC.getRooms(), bathWC.getHeights(), reqArea, reqWidth, reqHeight, scrutinyDetail);
    }

    /**
     * Validates the area, width, and height of bathroom water closets on a given floor.
     */
    private void validateBathroomWaterCloset(Plan plan, Floor floor, List<Measurement> rooms, List<RoomHeight> heights,
                                             BigDecimal reqArea, BigDecimal reqWidth, BigDecimal reqHeight,
                                             ScrutinyDetail scrutinyDetail) {

        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal minWidth = rooms.get(0).getWidth();
        BigDecimal minHeight = heights.get(0).getHeight();

        for (Measurement m : rooms) {
            totalArea = totalArea.add(m.getArea());
            if (m.getWidth().compareTo(minWidth) < 0) {
                minWidth = m.getWidth();
            }
        }

        for (RoomHeight rh : heights) {
            if (rh.getHeight().compareTo(minHeight) < 0) {
                minHeight = rh.getHeight();
            }
        }

        boolean isAccepted = minHeight.compareTo(reqHeight) >= 0
                && totalArea.compareTo(reqArea) >= 0
                && minWidth.compareTo(reqWidth) >= 0;

        LOG.debug("Bathroom WC validation on Floor Number: {} - Min Height: {}, Total Area: {}, Min Width: {}, " +
                  "Required Height: {}, Required Area: {}, Required Width: {}, Accepted: {}",
                floor.getNumber(), minHeight, totalArea, minWidth, reqHeight, reqArea, reqWidth, isAccepted);

        Map<String, String> resultRow = createResultRow(floor, reqArea, reqWidth, reqHeight, totalArea, minWidth, minHeight, isAccepted);
        scrutinyDetail.getDetail().add(resultRow);
    }

    /**
     * Creates and initializes a {@link ScrutinyDetail} object for bathroom water closet validation.
     */
    private ScrutinyDetail createScrutinyDetail() {
        LOG.debug("Creating ScrutinyDetail object for Bathroom Water Closets");
        ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
        scrutinyDetail.setKey(Common_Bathroom_Water_Closets);
        scrutinyDetail.addColumnHeading(1, RULE_NO);
        scrutinyDetail.addColumnHeading(2, DESCRIPTION);
        scrutinyDetail.addColumnHeading(3, REQUIRED);
        scrutinyDetail.addColumnHeading(4, PROVIDED);
        scrutinyDetail.addColumnHeading(5, STATUS);
        return scrutinyDetail;
    }

    /**
     * Creates a result row map containing the outcome of bathroom water closet validation for a given floor.
     */
    private Map<String, String> createResultRow(Floor floor, BigDecimal reqArea, BigDecimal reqWidth, BigDecimal reqHeight,
                                                BigDecimal totalArea, BigDecimal minWidth, BigDecimal minHeight, boolean isAccepted) {
        ReportScrutinyDetail detail = new ReportScrutinyDetail();
        detail.setRuleNo(RULE_41_IV);
        detail.setDescription(BathroomWaterClosets_DESCRIPTION);
        detail.setRequired(HEIGHT + reqHeight + TOTAL_AREA + reqArea + WIDTH + reqWidth);
        detail.setProvided(HEIGHT + minHeight + TOTAL_AREA + totalArea + WIDTH + minWidth);
        detail.setStatus(isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

        LOG.debug("Created result row for Floor Number: {} with acceptance: {}", floor.getNumber(), isAccepted);

        return mapReportDetails(detail);
    }

    /**
     * This method returns an empty map as no amendments are defined for this feature.
     */
    @Override
    public Map<String, Date> getAmendments() {
        LOG.debug("Returning empty amendments map");
        return new LinkedHashMap<>();
    }

}
