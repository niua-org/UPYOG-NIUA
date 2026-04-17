/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
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

package org.egov.common.entity.edcr;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;

import org.egov.common.entity.bpa.SubOccupancy;
import org.egov.common.entity.bpa.Usage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*All the details extracted from the plan are referred in this object*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanBpa implements Serializable {

    private static final long serialVersionUID = 7276648029097296311L;

    /**
     * Plan scrutiny report status. Values true mean "Accepted" and False mean "Not Accepted". Default value false. On plan
     * scrutiny, if all the rules are success then value is true.
     */
    Map<String, String> planInfoProperties = new HashMap<>();

    private Boolean edcrPassed = false;
    // Submission date of plan scrutiny.
    private Date applicationDate;
    /**
     * decides on what date scrutiny should be done
     */
    private Date asOnDate;
    private Plot plot;

    /**
     * Planinformation captures the declarations of the plan.Plan information captures the boundary, building location
     * details,surrounding building NOC's etc. User will assert the details about the plot. The same will be used to print in plan
     * report.
     */
    private PlanInformation planInformation;


    // Single plan contain multiple block/building information. Records Existing and proposed block information.
    private List<Block> blocks = new ArrayList<>();

    private String tenantId;

    // List of occupancies present in the plot including all the blocks.
    private List<Occupancy> occupancies = new ArrayList<>();
    @JsonIgnore
    private transient Map<Integer, org.egov.common.entity.bpa.Occupancy> occupanciesMaster = new HashMap<>();
    @JsonIgnore
    private transient Map<Integer, SubOccupancy> subOccupanciesMaster = new HashMap<>();

    // coverage Overall Coverage of all the block. Total area of all the floor/plot area.



    public List<Occupancy> getOccupancies() {
        return occupancies;
    }

    public void setOccupancies(List<Occupancy> occupancies) {
        this.occupancies = occupancies;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public Block getBlockByName(String blockName) {
        for (Block block : getBlocks()) {
            if (block.getName().equalsIgnoreCase(blockName))
                return block;
        }
        return null;
    }


    public Boolean getEdcrPassed() {
        return edcrPassed;
    }

    public void setEdcrPassed(Boolean edcrPassed) {
        this.edcrPassed = edcrPassed;
    }

    public Date getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(Date applicationDate) {
        this.applicationDate = applicationDate;
    }


    public PlanInformation getPlanInformation() {
        return planInformation;
    }

    public void setPlanInformation(PlanInformation planInformation) {
        this.planInformation = planInformation;
    }

    public Plot getPlot() {

        return plot;
    }

    public void setPlot(Plot plot) {
        this.plot = plot;
    }



    public void sortBlockByName() {
        if (!blocks.isEmpty())
            Collections.sort(blocks, Comparator.comparing(Block::getNumber));
    }

    public void sortSetBacksByLevel() {
        for (Block block : blocks)
            Collections.sort(block.getSetBacks(), Comparator.comparing(SetBack::getLevel));
    }


    public Map<Integer, org.egov.common.entity.bpa.Occupancy> getOccupanciesMaster() {
        return occupanciesMaster;
    }

    public void setOccupanciesMaster(Map<Integer, org.egov.common.entity.bpa.Occupancy> occupanciesMaster) {
        this.occupanciesMaster = occupanciesMaster;
    }

    public Map<Integer, SubOccupancy> getSubOccupanciesMaster() {
        return subOccupanciesMaster;
    }

    public void setSubOccupanciesMaster(Map<Integer, SubOccupancy> subOccupanciesMaster) {
        this.subOccupanciesMaster = subOccupanciesMaster;
    }



    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }



    public Map<String, String> getPlanInfoProperties() {
        return planInfoProperties;
    }

    public void setPlanInfoProperties(Map<String, String> planInfoProperties) {
        this.planInfoProperties = planInfoProperties;
    }



}
