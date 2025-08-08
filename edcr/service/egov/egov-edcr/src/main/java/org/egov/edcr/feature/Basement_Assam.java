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

import static org.egov.edcr.constants.CommonFeatureConstants.BETWEEN;
import static org.egov.edcr.constants.CommonFeatureConstants.GREATER_THAN_EQUAL;
import static org.egov.edcr.constants.CommonFeatureConstants.TO;
import static org.egov.edcr.constants.EdcrReportConstants.BASEMENT_DESCRIPTION_ONE;
import static org.egov.edcr.constants.EdcrReportConstants.BASEMENT_DESCRIPTION_TWO;
import static org.egov.edcr.constants.EdcrReportConstants.RULE_46_6A;
import static org.egov.edcr.constants.EdcrReportConstants.RULE_46_6C;
import static org.egov.edcr.constants.EdcrReportConstants.RULE_46_6C_DESCRIPTION;
import static org.egov.edcr.service.FeatureUtil.mapReportDetails;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.entity.edcr.BasementRequirement;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.FeatureEnum;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ReportScrutinyDetail;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.service.MDMSCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Basement_Assam extends Basement {

	private static final Logger LOG = LogManager.getLogger(Basement_Assam.class);

	@Autowired
	private MDMSCacheManager cache;

	@Override
	public Plan validate(Plan pl) {
	    
	    return pl;
	}

	@Override
	public Plan process(Plan pl) {
	   
	    if (pl.getBlocks() == null) {
	        LOG.warn("No blocks found in the plan, returning without processing");
	        return pl;
	    }

	    ScrutinyDetail scrutinyDetail = createScrutinyDetail();

	    Optional<BasementRequirement> matchedRule = fetchBasementRule(pl);
	    if (!matchedRule.isPresent()) {
	        LOG.warn("No basement rules found in MDMS cache for the plan");
	        return pl;
	    }

	    BasementRequirement rule = matchedRule.get();
	    LOG.info("Using basement rule with permissibleOne: {}, permissibleTwo: {}, permissibleThree: {}",
	            rule.getPermissibleOne(), rule.getPermissibleTwo(), rule.getPermissibleThree());

	    BigDecimal singleBasementHeight = rule.getPermissibleOne();
	    BigDecimal multiLevelHeight = rule.getPermissibleTwo();
	    BigDecimal mechanisedSplitHeight = rule.getPermissibleThree();

	    for (Block block : pl.getBlocks()) {
	        LOG.debug("Processing block ID: {}", block.getNumber());

	        if (!hasValidFloors(block)) {
	            LOG.debug("Block ID: {} has no valid floors, skipping", block.getNumber());
	            continue;
	        }

	        List<Floor> basementFloors = block.getBuilding().getFloors().stream()
	                .filter(floor -> floor.getNumber() == -1)
	                .collect(Collectors.toList());

	        if (basementFloors.isEmpty()) {
	            LOG.debug("No basement floors found in block ID: {}, skipping", block.getNumber());
	            continue;
	        }

	        if (basementFloors.size() == 1) {
	            LOG.debug("Single basement floor found in block ID: {}, validating height", block.getNumber());
	            validateHeightFromFloorToCeiling(basementFloors.get(0), singleBasementHeight, scrutinyDetail);
	        } else {
	            LOG.debug("Multiple basement floors found in block ID: {}, validating each floor", block.getNumber());
	            for (Floor floor : basementFloors) {
	                LOG.debug("Validating floor number {} in block ID: {}", floor.getNumber(), block.getNumber());
	                validateHeightFromFloorToCeiling(floor, multiLevelHeight, scrutinyDetail);

	                validateSplitLevelBasementParkingHeight(floor, mechanisedSplitHeight,
	                        RULE_46_6C, RULE_46_6C_DESCRIPTION, scrutinyDetail);
	            }
	        }

	        // Ceiling height rule validation (common)
	        for (Floor floor : basementFloors) {
	            LOG.debug("Validating upper basement ceiling height for floor number {} in block ID: {}",
	                    floor.getNumber(), block.getNumber());
	            validateUpperBasementCeilingHeight(floor, rule.getPermissibleTwo(), rule.getPermissibleThree(), scrutinyDetail);
	        }
	    }

	    pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	  
	    return pl;
	}

	private boolean hasValidFloors(Block block) {
	    boolean valid = block.getBuilding() != null
	            && block.getBuilding().getFloors() != null
	            && !block.getBuilding().getFloors().isEmpty();
	    LOG.debug("Block ID: {} has valid floors? {}", block.getNumber(), valid);
	    return valid;
	}

	private Optional<BasementRequirement> fetchBasementRule(Plan pl) {
	  
	    List<Object> rules = cache.getFeatureRules(pl, FeatureEnum.BASEMENT.getValue(), false);

	    return rules.stream()
	            .filter(BasementRequirement.class::isInstance)
	            .map(BasementRequirement.class::cast)
	            .findFirst();
	}

	private void validateSplitLevelBasementParkingHeight(Floor floor, BigDecimal requiredHeight, String ruleNo,
	                                                    String description, ScrutinyDetail detail) {
	    List<BigDecimal> splitHeights = floor.getSplitLevelBasementParkingHeights();
	    if (splitHeights == null || splitHeights.isEmpty()) {
	        LOG.debug("No split level basement parking heights found for floor number: {}", floor.getNumber());
	        return;
	    }

	    BigDecimal minSplitHeight = splitHeights.stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);
	    boolean accepted = minSplitHeight.compareTo(requiredHeight) >= 0;
	    LOG.debug("Split level basement parking height validation for floor {}: min height = {}, required = {}, accepted = {}",
	            floor.getNumber(), minSplitHeight, requiredHeight, accepted);

	    detail.getDetail().add(createResultRow(
	            ruleNo,
	            description,
	            ">=" + requiredHeight,
	            minSplitHeight,
	            accepted
	    ));
	}

	private ScrutinyDetail createScrutinyDetail() {
	    LOG.debug("Creating ScrutinyDetail object for Basement rules");
	    ScrutinyDetail detail = new ScrutinyDetail();
	    detail.setKey(Common_Basement);
	    detail.addColumnHeading(1, RULE_NO);
	    detail.addColumnHeading(2, DESCRIPTION);
	    detail.addColumnHeading(3, REQUIRED);
	    detail.addColumnHeading(4, PROVIDED);
	    detail.addColumnHeading(5, STATUS);
	    return detail;
	}

	private void validateHeightFromFloorToCeiling(Floor floor, BigDecimal basementValue, ScrutinyDetail detail) {
	    List<BigDecimal> heights = floor.getHeightFromTheFloorToCeiling();
	    if (heights == null || heights.isEmpty()) {
	        LOG.debug("No heights from floor to ceiling provided for floor number: {}", floor.getNumber());
	        return;
	    }

	    BigDecimal minHeight = heights.stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);
	    boolean accepted = minHeight.compareTo(basementValue) >= 0;
	    LOG.debug("Height from floor to ceiling validation for floor {}: min height = {}, required = {}, accepted = {}",
	            floor.getNumber(), minHeight, basementValue, accepted);

	    detail.getDetail().add(createResultRow(
	            RULE_46_6A,
	            BASEMENT_DESCRIPTION_ONE,
	            GREATER_THAN_EQUAL + basementValue,
	            minHeight,
	            accepted
	    ));
	}

	private void validateUpperBasementCeilingHeight(Floor floor, BigDecimal minValue, BigDecimal maxValue, ScrutinyDetail detail) {
	    List<BigDecimal> ceilingHeights = floor.getHeightOfTheCeilingOfUpperBasement();
	    if (ceilingHeights == null || ceilingHeights.isEmpty()) {
	        LOG.debug("No ceiling heights for upper basement found for floor number: {}", floor.getNumber());
	        return;
	    }

	    BigDecimal minCeilingHeight = ceilingHeights.stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);
	    boolean accepted = minCeilingHeight.compareTo(minValue) >= 0 && minCeilingHeight.compareTo(maxValue) < 0;
	    LOG.debug("Upper basement ceiling height validation for floor {}: min ceiling height = {}, required range = {} to {}, accepted = {}",
	            floor.getNumber(), minCeilingHeight, minValue, maxValue, accepted);

	    detail.getDetail().add(createResultRow(
	            RULE_46_6C,
	            BASEMENT_DESCRIPTION_TWO,
	            BETWEEN + minValue + TO + maxValue,
	            minCeilingHeight,
	            accepted
	    ));
	}

	private Map<String, String> createResultRow(String ruleNo, String description, String required, BigDecimal provided, boolean accepted) {
	    ReportScrutinyDetail detail = new ReportScrutinyDetail();
	    detail.setRuleNo(ruleNo);
	    detail.setDescription(description);
	    detail.setRequired(required);
	    detail.setProvided(provided.toString());
	    detail.setStatus(accepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

	    LOG.debug("Created result row for rule: {}, accepted: {}", ruleNo, accepted);

	    return mapReportDetails(detail);
	}

	@Override
	public Map<String, Date> getAmendments() {
	    LOG.debug("Returning empty amendments map");
	    return new LinkedHashMap<>();
	}
}
