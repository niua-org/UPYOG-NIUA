package org.egov.edcr.feature;

import static org.egov.edcr.utility.DcrConstants.BUILDING_HEIGHT;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.SHORTESTDISTINACETOBUILDINGFOOTPRINT;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.SetBack;
import org.egov.edcr.entity.blackbox.BuildingDetail;
import org.egov.edcr.entity.blackbox.MeasurementDetail;
import org.egov.edcr.entity.blackbox.PlanDetail;
import org.egov.edcr.service.LayerNames;
import org.egov.edcr.utility.DcrConstants;
import org.egov.edcr.utility.Util;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFLWPolyline;
import org.kabeja.dxf.DXFLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FurthestCornerOfBuildingExtract extends FeatureExtract {
	   private static final Logger LOG = LogManager.getLogger(FurthestCornerOfBuildingExtract.class);
	    public static final String UPTO = "Up To";
	    public static final String DECLARED = "Declared";
	    private static final String MANYLINES = "Multiple Lines Defined";
	    private static final String SLOPE = "SLOPE OF LINE";
	    public static final String AVG_GROUND_LEVEL = "AVG_GROUND_LVL";
	    public static final String ROOF_LEVEL = "ROOF_LVL";
	    
	    @Autowired
	    private LayerNames layerNames;

	    @Override
	    public PlanDetail extract(PlanDetail pl) {
	        if (LOG.isInfoEnabled())
	            LOG.info("Starting of Furthest Corner Of Building Extract......");
	        extractFurthestCornerOfTheBuilding(pl);
	        if (LOG.isInfoEnabled())
	            LOG.info("End of urthest Corner Of Building Extract......");
	        return pl;

	    }

	    private void extractFurthestCornerOfTheBuilding(PlanDetail pl) {
	    
	    		for (Block b : pl.getBlocks()) {
	    			String layerName = layerNames.getLayerName("LAYER_NAME_BLOCK_NAME_PREFIX") + b.getNumber() + "_"
	    					+ layerNames.getLayerName("LAYER_NAME_FURTHEST_COR_OF_BUILDING");
	    			BigDecimal furthestCorner = Util.getSingleDimensionValueByLayer(pl.getDoc(), layerName, pl);
	    	
	    			b.getBuilding().setFurthestCornerOfTheBuilding(furthestCorner);
	    		
	    		
	    	}
	    }
	    @Override
	    public PlanDetail validate(PlanDetail pl) {
	        return pl;
	    }
	

}
