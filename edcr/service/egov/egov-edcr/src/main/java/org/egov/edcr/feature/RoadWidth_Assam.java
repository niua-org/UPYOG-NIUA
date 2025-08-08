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

import static org.egov.edcr.constants.CommonFeatureConstants.METER;
import static org.egov.edcr.constants.EdcrReportConstants.TYPE_ULB;
import static org.egov.edcr.constants.EdcrReportConstants.TYPE_OUTSIDE_ULB;
import static org.egov.edcr.service.FeatureUtil.addScrutinyDetailtoPlan;
import static org.egov.edcr.service.FeatureUtil.mapReportDetails;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.entity.edcr.FeatureEnum;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ReportScrutinyDetail;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.RoadWidthRequirement;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.service.MDMSCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service


public class RoadWidth_Assam extends RoadWidth {

    private static final Logger LOG = LogManager.getLogger(RoadWidth_Assam.class);
    private static final String RULE_34 = "34-1";
    public static final String ROADWIDTH_DESCRIPTION = "Minimum Road Width";
    public static final String NEW = "NEW";
   

    @Autowired
    MDMSCacheManager cache;

    @Override
    public Map<String, Date> getAmendments() {
        return new LinkedHashMap<>();
    }

    @Override
    public Plan validate(Plan pl) {
        return pl;
    }

    @Override
    public Plan process(Plan pl) {
        LOG.info("Starting road width validation process");

        if (pl.getPlanInformation() == null || pl.getPlanInformation().getRoadWidth() == null) {
            LOG.warn("Plan information or road width is null. Skipping process.");
            return pl;
        }

        BigDecimal roadWidth = pl.getPlanInformation().getRoadWidth();
        String typeOfArea = pl.getPlanInformation().getTypeOfArea(); // Can be NEW, ULB, OUTSIDE_ULB, etc.

        LOG.debug("Road width: {}, Type of area: {}", roadWidth, typeOfArea);

        if (!NEW.equalsIgnoreCase(typeOfArea)) {
            LOG.info("Type of area is not NEW. Skipping road width validation.");
            return pl;
        }

        ScrutinyDetail scrutinyDetail = buildRoadWidthScrutinyDetail();
        LOG.debug("Created scrutiny detail for road width validation.");

        ReportScrutinyDetail detail = new ReportScrutinyDetail();
        detail.setRuleNo(RULE_34);
        detail.setDescription(ROADWIDTH_DESCRIPTION);

        if (pl.getVirtualBuilding() == null || pl.getVirtualBuilding().getMostRestrictiveFarHelper() == null) {
            LOG.warn("Virtual building or most restrictive FAR helper is null. Skipping road width validation.");
            return pl;
        }

        OccupancyTypeHelper occupancy = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
        String occupancyCode = occupancy.getType() != null ? occupancy.getType().getCode() : null;
        String subOccupancyCode = occupancy.getSubtype() != null ? occupancy.getSubtype().getCode() : null;

        LOG.debug("Occupancy code: {}, Sub occupancy code: {}", occupancyCode, subOccupancyCode);

        detail.setOccupancy(occupancy.getSubtype() != null ? occupancy.getSubtype().getName() : occupancy.getType().getName());

        // Fetch values from MDMS
        BigDecimal requiredRoadWidth = getRequiredRoadWidthFromMDMS(pl, occupancyCode, subOccupancyCode);
        LOG.debug("Required road width from MDMS: {}", requiredRoadWidth);

        if (requiredRoadWidth != null) {
            detail.setPermitted(requiredRoadWidth + METER);
            detail.setProvided(roadWidth + METER);

            String status = roadWidth.compareTo(requiredRoadWidth) >= 0
                    ? Result.Accepted.getResultVal()
                    : Result.Not_Accepted.getResultVal();

            detail.setStatus(status);

            Map<String, String> details = mapReportDetails(detail);

            LOG.info("Road width comparison - Provided: {}, Required: {}, Status: {}", roadWidth, requiredRoadWidth, status);

            addScrutinyDetailtoPlan(scrutinyDetail, pl, details);
            LOG.debug("Added scrutiny detail to plan report.");
        } else {
            LOG.warn("No required road width found in MDMS for occupancy: {}, sub-occupancy: {}", occupancyCode, subOccupancyCode);
        }

        LOG.info("Completed road width validation process");
        return pl;
    }

    /**
     * Fetches the permissible road width based on occupancy and sub-occupancy
     */
    public BigDecimal getRequiredRoadWidthFromMDMS(Plan plan, String occupancy, String subOccupancy) {
        LOG.debug("Fetching required road width from MDMS for occupancy: {}, subOccupancy: {}", occupancy, subOccupancy);

        List<Object> rules = cache.getFeatureRules(plan, FeatureEnum.ROAD_WIDTH.getValue(), false);

        String typeOfArea = plan.getPlanInformation().getTypeOfArea(); // ULB or OUTSIDE_ULB
        LOG.debug("Plan typeOfArea for road width fetch: {}", typeOfArea);

        for (Object rule : rules) {
            if (rule instanceof RoadWidthRequirement) {
                RoadWidthRequirement rwr = (RoadWidthRequirement) rule;

                boolean matchesOccupancy = rwr.getOccupancy() != null && rwr.getOccupancy().equalsIgnoreCase(occupancy);

                // match sub-occupancy exactly, or allow rule to be generic (null sub-occupancy)
                boolean matchesSubOccupancy =
                        rwr.getSubOccupancy() == null ||
                        rwr.getSubOccupancy().equalsIgnoreCase(subOccupancy);

                if (matchesOccupancy && matchesSubOccupancy) {
                    BigDecimal permissible = null;

                    if (TYPE_ULB.equalsIgnoreCase(typeOfArea)) {
                        permissible = rwr.getPermissibleULB();
                    } else if (TYPE_OUTSIDE_ULB.equalsIgnoreCase(typeOfArea)) {
                        permissible = rwr.getPermissibleOutsideULB();
                    }

                    // fallback to default permissible if specific ones are not set
                    if (permissible == null) {
                        permissible = rwr.getPermissible();
                    }

                    LOG.debug("Matched RoadWidthRequirement: occupancy={}, subOccupancy={}, permissible={}",
                            occupancy, subOccupancy, permissible);

                    return permissible;
                }
            }
        }

        LOG.warn("No matching road width rule found for occupancy: {}, subOccupancy: {}", occupancy, subOccupancy);

        return null; // No matching rule found
    }

    private ScrutinyDetail buildRoadWidthScrutinyDetail() {
        LOG.debug("Building ScrutinyDetail object for Road Width");
        ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
        scrutinyDetail.setKey(Common_Road_Width);
        scrutinyDetail.addColumnHeading(1, RULE_NO);
        scrutinyDetail.addColumnHeading(2, DESCRIPTION);
        scrutinyDetail.addColumnHeading(3, OCCUPANCY);
        scrutinyDetail.addColumnHeading(4, PERMITTED);
        scrutinyDetail.addColumnHeading(5, PROVIDED);
        scrutinyDetail.addColumnHeading(6, STATUS);
        return scrutinyDetail;
    }

}


