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

import static org.egov.edcr.constants.CommonFeatureConstants.HEIGHT_AREA_WIDTH_EQUAL_TO_S;
import static org.egov.edcr.constants.CommonFeatureConstants.HEIGHT_AREA_WIDTH_GREATER_THAN_S;
import static org.egov.edcr.constants.CommonFeatureConstants.WATER_CLOSET_VENTILATION_AREA;
import static org.egov.edcr.constants.CommonKeyConstants.COMMON_WATER_CLOSETS;
import static org.egov.edcr.constants.CommonKeyConstants.WATER_CLOSETS_VENTILATION;
import static org.egov.edcr.constants.EdcrReportConstants.RULE_41_IV;
import static org.egov.edcr.constants.EdcrReportConstants.RULE_91_D;
import static org.egov.edcr.constants.EdcrReportConstants.WATERCLOSETS_DESCRIPTION;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_AREA_DESC;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_AREA_UNIT;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_AT_FLOOR;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_MISSING_DESC;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_NOT_AVAILABLE;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_NOT_DEFINED;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_WIDTH_DESC;
import static org.egov.edcr.constants.EdcrReportConstants.WC_VENTILATION_WIDTH_UNIT;
import static org.egov.edcr.service.FeatureUtil.mapReportDetails;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.FeatureEnum;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ReportScrutinyDetail;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.RoomHeight;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.WaterClosetsRequirement;
import org.egov.edcr.service.MDMSCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WaterClosets_Assam extends WaterClosets {

	private static final Logger LOG = LogManager.getLogger(WaterClosets_Assam.class);

	/**
	 * Validates the building plan for water closet requirements.
	 * Currently performs no validation and returns the plan as-is.
	 *
	 * @param pl The building plan to validate
	 * @return The unmodified plan
	 */
	@Override
	public Plan validate(Plan pl) {
		return pl;
	}
	
	@Autowired
	MDMSCacheManager cache;
	/**
	 * Processes water closet requirements for all blocks and floors in the building plan.
	 * Creates scrutiny details for dimensions and ventilation, fetches water closet rules
	 * from MDMS, and validates height, width, area, and ventilation requirements.
	 *
	 * @param pl The building plan to process
	 * @return The processed plan with scrutiny details added
	 */
	@Override
	public Plan process(Plan pl) {
	    LOG.info("Starting process for Water Closets in the plan");

	    ScrutinyDetail dimScrutinyDetail = createScrutinyDetail(COMMON_WATER_CLOSETS);
	    ScrutinyDetail ventScrutinyDetail = createScrutinyDetail(WATER_CLOSETS_VENTILATION);

	    Optional<WaterClosetsRequirement> matchedRule = getWaterClosetsRule(pl);

	    if (!matchedRule.isPresent()) {
	        LOG.warn("No WaterClosetsRequirement rule found in MDMS for the plan");
	        return pl;
	    }

	    WaterClosetsRequirement wcRule = matchedRule.get();
	    if (wcRule == null) {
	        LOG.warn("WaterClosetsRequirement rule retrieved as null, returning plan as is");
	        return pl;
	    }

	    LOG.debug("Using WaterClosetsRequirement rule with Height: {}, Area: {}, Width: {}",
	            wcRule.getWaterClosetsHeight(), wcRule.getWaterClosetsArea(), wcRule.getWaterClosetsWidth());

	    for (Block block : pl.getBlocks()) {
	        if (block.getBuilding() == null || block.getBuilding().getFloors() == null) {
	            LOG.debug("Skipping block {} as building or floors are null", block.getNumber());
	            continue;
	        }

	        for (Floor floor : block.getBuilding().getFloors()) {
	            if (!hasValidWaterClosets(floor)) {
	                LOG.debug("Skipping floor {} in block {} due to invalid water closet data", floor.getNumber(), block.getNumber());
	                continue;
	            }

	            BigDecimal minHeight = getMinHeight(floor.getWaterClosets().getHeights());
	            BigDecimal minWidth = getMinWidth(floor.getWaterClosets().getRooms());
	            BigDecimal totalArea = getTotalArea(floor.getWaterClosets().getRooms());

	            LOG.debug("Floor {} in block {} - Min Height: {}, Min Width: {}, Total Area: {}",
	                    floor.getNumber(), block.getNumber(), minHeight, minWidth, totalArea);

	            processWCVentilation(floor, ventScrutinyDetail, pl);

	            Map<String, String> dimDetails = buildDimensionDetails(
	                    wcRule.getWaterClosetsHeight(), wcRule.getWaterClosetsArea(), wcRule.getWaterClosetsWidth(),
	                    minHeight, totalArea, minWidth
	            );
	            dimScrutinyDetail.getDetail().add(dimDetails);
	            LOG.debug("Added dimension validation details for floor {} in block {}", floor.getNumber(), block.getNumber());
	        }
	    }

	    pl.getReportOutput().getScrutinyDetails().add(dimScrutinyDetail);
	    pl.getReportOutput().getScrutinyDetails().add(ventScrutinyDetail);

	    LOG.info("Completed processing water closets for the plan");
	    return pl;
	}

	private void processWCVentilation(Floor floor, ScrutinyDetail scrutinyDetail, Plan pl) {
	    LOG.debug("Processing Water Closet Ventilation validation for floor {}", floor.getNumber());

	    List<Measurement> wcVentilation = floor.getWaterClosets().getWaterClosetVentilation();

	    if (wcVentilation == null || wcVentilation.isEmpty()) {
	        LOG.warn("Water closet ventilation missing for floor {}", floor.getNumber());

	        ReportScrutinyDetail detail = new ReportScrutinyDetail();
	        detail.setRuleNo(RULE_91_D);
	        detail.setDescription(WC_VENTILATION_MISSING_DESC);
	        detail.setRequired(WC_VENTILATION_NOT_DEFINED);
	        detail.setProvided(WC_VENTILATION_NOT_AVAILABLE + floor.getNumber());
	        detail.setStatus(Result.Not_Accepted.getResultVal());

	        scrutinyDetail.getDetail().add(mapReportDetails(detail));
	        pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	        return;
	    }

	    BigDecimal requiredArea = BigDecimal.ZERO;
	    BigDecimal requiredWidth = BigDecimal.ZERO;

	    List<Object> rules = cache.getFeatureRules(pl, FeatureEnum.WATER_CLOSETS.getValue(), false);
	    Optional<WaterClosetsRequirement> matchedRule = rules.stream()
	            .filter(WaterClosetsRequirement.class::isInstance)
	            .map(WaterClosetsRequirement.class::cast)
	            .findFirst();

	    if (matchedRule.isPresent()) {
	        WaterClosetsRequirement rule = matchedRule.get();
	        requiredArea = rule.getWaterClosetsVentilationArea();
	        requiredWidth = rule.getWaterClosetsVentilationWidth();

	        LOG.debug("Water closet ventilation requirements from rule - Area: {}, Width: {}", requiredArea, requiredWidth);
	    } else {
	        LOG.warn("No water closet ventilation rules found in cache");
	    }

	    BigDecimal providedArea = wcVentilation.stream()
	            .map(Measurement::getArea)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);

	    BigDecimal providedWidth = wcVentilation.stream()
	            .map(Measurement::getWidth)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);

	    LOG.debug("Provided ventilation area: {}, width: {} for floor {}", providedArea, providedWidth, floor.getNumber());

	    // Area validation
	    if (requiredArea.compareTo(BigDecimal.ZERO) > 0) {
	        ReportScrutinyDetail detail = new ReportScrutinyDetail();
	        detail.setRuleNo(RULE_91_D);
	        detail.setDescription(WC_VENTILATION_AREA_DESC);
	        detail.setRequired(requiredArea + WC_VENTILATION_AREA_UNIT);
	        detail.setProvided(providedArea + WC_VENTILATION_AREA_UNIT + WC_VENTILATION_AT_FLOOR + floor.getNumber());
	        detail.setStatus(providedArea.compareTo(requiredArea) >= 0
	                ? Result.Accepted.getResultVal()
	                : Result.Not_Accepted.getResultVal());

	        scrutinyDetail.getDetail().add(mapReportDetails(detail));
	    }

	    // Width validation
	    if (requiredWidth.compareTo(BigDecimal.ZERO) > 0) {
	        ReportScrutinyDetail detail = new ReportScrutinyDetail();
	        detail.setRuleNo(RULE_91_D);
	        detail.setDescription(WC_VENTILATION_WIDTH_DESC);
	        detail.setRequired(requiredWidth + WC_VENTILATION_WIDTH_UNIT);
	        detail.setProvided(providedWidth + WC_VENTILATION_WIDTH_UNIT + WC_VENTILATION_AT_FLOOR + floor.getNumber());
	        detail.setStatus(providedWidth.compareTo(requiredWidth) >= 0
	                ? Result.Accepted.getResultVal()
	                : Result.Not_Accepted.getResultVal());

	        scrutinyDetail.getDetail().add(mapReportDetails(detail));
	    }

	    pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	    LOG.debug("Completed Water Closet Ventilation validation for floor {}", floor.getNumber());
	}

	private ScrutinyDetail createScrutinyDetail(String key) {
	    LOG.debug("Creating ScrutinyDetail with key: {}", key);
	    ScrutinyDetail detail = new ScrutinyDetail();
	    detail.setKey(key);
	    detail.addColumnHeading(1, RULE_NO);
	    detail.addColumnHeading(2, DESCRIPTION);
	    detail.addColumnHeading(3, REQUIRED);
	    detail.addColumnHeading(4, PROVIDED);
	    detail.addColumnHeading(5, STATUS);
	    return detail;
	}

	private Optional<WaterClosetsRequirement> getWaterClosetsRule(Plan pl) {
	    LOG.debug("Fetching WaterClosetsRequirement from MDMS cache");
	    List<Object> rules = cache.getFeatureRules(pl, FeatureEnum.WATER_CLOSETS.getValue(), false);
	    Optional<WaterClosetsRequirement> ruleOpt = rules.stream()
	            .filter(WaterClosetsRequirement.class::isInstance)
	            .map(WaterClosetsRequirement.class::cast)
	            .findFirst();
	    if (!ruleOpt.isPresent())
	        LOG.warn("No WaterClosetsRequirement rules found");
	    return ruleOpt;
	}

	private boolean hasValidWaterClosets(Floor floor) {
	    boolean hasValid = floor.getWaterClosets() != null
	            && floor.getWaterClosets().getHeights() != null
	            && !floor.getWaterClosets().getHeights().isEmpty()
	            && floor.getWaterClosets().getRooms() != null
	            && !floor.getWaterClosets().getRooms().isEmpty();

	    LOG.debug("Floor {} has valid water closets? {}", floor.getNumber(), hasValid);
	    return hasValid;
	}

	private BigDecimal getMinHeight(List<RoomHeight> heights) {
	    BigDecimal minHeight = heights.stream()
	            .map(RoomHeight::getHeight)
	            .min(Comparator.naturalOrder())
	            .orElse(BigDecimal.ZERO);
	    LOG.debug("Computed minimum height from heights list: {}", minHeight);
	    return minHeight;
	}

	private BigDecimal getMinWidth(List<Measurement> rooms) {
	    BigDecimal minWidth = rooms.stream()
	            .map(Measurement::getWidth)
	            .min(Comparator.naturalOrder())
	            .orElse(BigDecimal.ZERO);
	    LOG.debug("Computed minimum width from rooms list: {}", minWidth);
	    return minWidth;
	}

	private BigDecimal getTotalArea(List<Measurement> rooms) {
	    BigDecimal totalArea = rooms.stream()
	            .map(Measurement::getArea)
	            .reduce(BigDecimal.ZERO, BigDecimal::add);
	    LOG.debug("Computed total area from rooms list: {}", totalArea);
	    return totalArea;
	}

	private Map<String, String> buildVentilationDetails(BigDecimal requiredVentArea, BigDecimal providedVentArea) {
	    ReportScrutinyDetail detail = new ReportScrutinyDetail();
	    detail.setRuleNo(RULE_41_IV);
	    detail.setDescription(WATERCLOSETS_DESCRIPTION);
	    detail.setRequired(requiredVentArea.toString());
	    detail.setProvided(WATER_CLOSET_VENTILATION_AREA + providedVentArea);
	    detail.setStatus(providedVentArea.compareTo(requiredVentArea) >= 0
	            ? Result.Accepted.getResultVal()
	            : Result.Not_Accepted.getResultVal());
	    return mapReportDetails(detail);
	}

	private Map<String, String> buildDimensionDetails(BigDecimal requiredHeight, BigDecimal requiredArea, BigDecimal requiredWidth,
	                                                  BigDecimal providedHeight, BigDecimal providedArea, BigDecimal providedWidth) {
	    ReportScrutinyDetail detail = new ReportScrutinyDetail();
	    detail.setRuleNo(RULE_41_IV);
	    detail.setDescription(WATERCLOSETS_DESCRIPTION);
	    detail.setRequired(String.format(HEIGHT_AREA_WIDTH_GREATER_THAN_S,
	            requiredHeight, requiredArea, requiredWidth));
	    detail.setProvided(String.format(HEIGHT_AREA_WIDTH_EQUAL_TO_S, providedHeight, providedArea, providedWidth));
	    boolean accepted = (providedHeight.compareTo(requiredHeight) >= 0
	            && providedArea.compareTo(requiredArea) >= 0
	            && providedWidth.compareTo(requiredWidth) >= 0);
	    detail.setStatus(accepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

	    LOG.debug("Built dimension validation detail: requiredHeight={}, requiredArea={}, requiredWidth={}, providedHeight={}, providedArea={}, providedWidth={}, accepted={}",
	            requiredHeight, requiredArea, requiredWidth, providedHeight, providedArea, providedWidth, accepted);

	    return mapReportDetails(detail);
	}


	/**
	 * Returns amendment dates for water closet rules.
	 * Currently returns an empty map as no amendments are defined.
	 *
	 * @return Empty LinkedHashMap of amendment dates
	 */
	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
