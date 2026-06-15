package org.egov.echallancalculation.service;


import java.util.*;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.echallancalculation.config.ChallanConfiguration;
import org.egov.echallancalculation.model.Amount;
import org.egov.echallancalculation.model.Challan;
import org.egov.echallancalculation.util.CalculationUtils;
import org.egov.echallancalculation.web.models.calculation.Calculation;
import org.egov.echallancalculation.web.models.calculation.CalculationReq;
import org.egov.echallancalculation.web.models.calculation.CalulationCriteria;
import org.egov.echallancalculation.web.models.demand.TaxHeadEstimate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CalculationService {

	
	@Autowired
	private DemandService demandService;
	
	@Autowired
	private CalculationUtils utils;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ChallanConfiguration config;

	@Autowired
	private RestTemplate restTemplate;

	/**
	 * Get CalculationReq and Calculate the Tax Head on challan
	 */
	public List<Calculation> getCalculation(CalculationReq request) {
		
		List<Calculation> calculations = new ArrayList<Calculation>();
		calculations = getCalculation(request.getRequestInfo(),request.getCalulationCriteria());
		for(CalulationCriteria criteria : request.getCalulationCriteria()){
			String applicationStatus = criteria.getChallan().getApplicationStatus();
			if(applicationStatus.equalsIgnoreCase("CANCELLED"))
				cancelBill(request.getRequestInfo(),criteria.getChallan());
		}
		demandService.generateDemand(request.getRequestInfo(), calculations,  request.getCalulationCriteria().get(0).getChallan().getBusinessService());
		return calculations;
	}
	
	//Creating calculation using amount entered by user
	public List<Calculation> getCalculation(RequestInfo requestInfo, List<CalulationCriteria> criterias){
	      List<Calculation> calculations = new LinkedList<>();
	      for(CalulationCriteria criteria : criterias) {
	          Challan challan = criteria.getChallan();
	          if (criteria.getChallan()==null && criteria.getChallanNo() != null) {
	              challan = utils.getChallan(requestInfo, criteria.getChallanNo(), criteria.getTenantId());
	              criteria.setChallan(challan);
	          }
	          
	          List<TaxHeadEstimate> estimates = new LinkedList<>();
	          
	          // Calculate amount based on challanAmount (user-entered), amount array (MDMS values), and MDMS rate (fetched)
	          // Demand will be generated with the highest value among these three sources
	          BigDecimal finalAmount = BigDecimal.ZERO;
	          String taxHeadCode = "CH.CHALLAN_FINE"; // Default tax head code
	          
	          // Get user-entered challan amount
	          BigDecimal challanAmount = BigDecimal.ZERO;
	          if (challan.getChallanAmount() != null && !challan.getChallanAmount().isEmpty()) {
	              try {
	                  challanAmount = new BigDecimal(challan.getChallanAmount());
	              } catch (NumberFormatException e) {
	                  log.warn("Invalid challanAmount format: {}", challan.getChallanAmount());
	              }
	          }
	          
	          // Get maximum amount from amount array (contains MDMS values)
	          BigDecimal amountArrayMax = BigDecimal.ZERO;
	          String amountArrayTaxHeadCode = null;
	          List<Amount> amountList = challan.getAmount();
	          if (amountList != null && !amountList.isEmpty()) {
	              for (Amount amountItem : amountList) {
	                  if (amountItem.getAmount() != null) {
	                      int comparison = amountItem.getAmount().compareTo(amountArrayMax);
	                      if (comparison > 0) {
	                          // Found a new maximum, update both amount and taxHeadCode
	                          amountArrayMax = amountItem.getAmount();
	                          if (amountItem.getTaxHeadCode() != null && !amountItem.getTaxHeadCode().isEmpty()) {
	                              amountArrayTaxHeadCode = amountItem.getTaxHeadCode();
	                          }
	                      } else if (comparison == 0 && amountArrayTaxHeadCode == null) {
	                          // Same maximum value, use taxHeadCode if we don't have one yet
	                          if (amountItem.getTaxHeadCode() != null && !amountItem.getTaxHeadCode().isEmpty()) {
	                              amountArrayTaxHeadCode = amountItem.getTaxHeadCode();
	                          }
	                      }
	                  }
	              }
	          }
	          
          // Get MDMS rate by fetching from MDMS service (if offence type is available)
          BigDecimal mdmsRate = BigDecimal.ZERO;
          String offenceTypeId = null;
          
          // Try to get offence type ID from offenceTypeName first, then from offenceType field
          if (challan.getOffenceTypeName() != null && !challan.getOffenceTypeName().isEmpty()) {
              offenceTypeId = utils.mapOffenceTypeNameToId(requestInfo, challan.getTenantId(), challan.getOffenceTypeName());
          } else if (challan.getOffenceType() != null && !challan.getOffenceType().isEmpty()) {
              offenceTypeId = challan.getOffenceType();
          }
          
          if (offenceTypeId != null) {
              mdmsRate = utils.getRateFromMDMS(requestInfo, challan.getTenantId(), offenceTypeId);
              // Fetch tax head code from OffenceType master
              if (challan.getOffenceTypeName() != null && !challan.getOffenceTypeName().isEmpty()) {
                  String mdmsTaxHeadCode = utils.getTaxHeadCodeFromOffenceTypeName(requestInfo, challan.getTenantId(), challan.getOffenceTypeName());
                  if (mdmsTaxHeadCode != null && !mdmsTaxHeadCode.isEmpty()) {
                      taxHeadCode = mdmsTaxHeadCode;
                  }
              }
          }
	          
	          // Compare all three values and use the highest
	          // Sources: 1) challanAmount (user-entered), 2) amountArrayMax (MDMS from array), 3) mdmsRate (MDMS fetched)
	          // Priority: challanAmount > amountArrayMax > mdmsRate (if values are equal, prefer in this order)
	          BigDecimal maxAmount = challanAmount.max(amountArrayMax).max(mdmsRate);
	          
	          // Determine which source has the highest value to set appropriate taxHeadCode
	          if (maxAmount.compareTo(BigDecimal.ZERO) > 0) {
	              // Check which value is the maximum, with priority order
	              if (challanAmount.compareTo(maxAmount) >= 0 && challanAmount.compareTo(BigDecimal.ZERO) > 0) {
	                  // challanAmount is the highest (or equal to max)
	                  finalAmount = challanAmount;
	                  // Use MDMS taxHeadCode or default
	              } else if (amountArrayMax.compareTo(maxAmount) >= 0 && amountArrayMax.compareTo(BigDecimal.ZERO) > 0) {
	                  // amount array has the highest value (or equal to max, and challanAmount is not higher)
	                  finalAmount = amountArrayMax;
	                  if (amountArrayTaxHeadCode != null && !amountArrayTaxHeadCode.isEmpty()) {
	                      taxHeadCode = amountArrayTaxHeadCode;
	                  }
	              } else if (mdmsRate.compareTo(maxAmount) >= 0 && mdmsRate.compareTo(BigDecimal.ZERO) > 0) {
	                  // MDMS rate is the highest (or equal to max, and others are not higher)
	                  finalAmount = mdmsRate;
	              } else {
	                  // Use the max amount (shouldn't reach here, but safety check)
	                  finalAmount = maxAmount;
	              }
	              
	              TaxHeadEstimate estimate = new TaxHeadEstimate();
	              estimate.setEstimateAmount(finalAmount);
	              estimate.setTaxHeadCode(taxHeadCode);
	              estimates.add(estimate);
	              
	              log.info("Demand calculation - challanAmount: {}, amountArrayMax: {}, mdmsRate: {}, finalAmount: {}", 
	                      challanAmount, amountArrayMax, mdmsRate, finalAmount);
	          } else {
	              // Fallback: if all are zero, use amount list as-is (preserve original behavior)
	              if (amountList != null && !amountList.isEmpty()) {
	                  for(Amount amountItem : amountList) {
	                      TaxHeadEstimate estimate = new TaxHeadEstimate();
	                      estimate.setEstimateAmount(amountItem.getAmount());
	                      estimate.setTaxHeadCode(amountItem.getTaxHeadCode() != null ? amountItem.getTaxHeadCode() : taxHeadCode);
	                      estimates.add(estimate);
	                  }
	              }
	          }
	          
	          Calculation calculation = new Calculation();
	          calculation.setChallan(criteria.getChallan());
	          calculation.setTenantId(criteria.getTenantId());
	          calculation.setTaxHeadEstimates(estimates);

	          calculations.add(calculation);

	      }
	      return calculations;
	  }

	/**
	 * Test method to demonstrate dynamic calculation with provided payload
	 * This method shows how the higher amount logic works with dynamic MDMS mapping
	 */
	public void testCalculationWithPayload(RequestInfo requestInfo, String tenantId) {
		log.info("=== Testing Dynamic Calculation with Provided Payload ===");
		
		// Example from your payload:
		// "offenceTypeName": "Loud Music After 10 PM"
		// "challanAmount": "200"
		
		String offenceTypeName = "Loud Music After 10 PM";
		String challanAmount = "200";
		
		// Dynamically map offence type name to ID from MDMS
		String offenceTypeId = utils.mapOffenceTypeNameToId(requestInfo, tenantId, offenceTypeName);
		log.info("Offence Type: {} -> Dynamically mapped to ID: {}", offenceTypeName, offenceTypeId);
		
		// Parse user amount
		BigDecimal userAmount = new BigDecimal(challanAmount);
		log.info("User entered challan amount: {}", userAmount);
		
		// Dynamically fetch MDMS rate
		BigDecimal mdmsRate = BigDecimal.ZERO;
		String taxHeadCode = "CH.CHALLAN_FINE";
		if (offenceTypeId != null) {
			mdmsRate = utils.getRateFromMDMS(requestInfo, tenantId, offenceTypeId);
			log.info("MDMS rate for {}: {}", offenceTypeId, mdmsRate);
			
			// Fetch tax head code from OffenceType master
			taxHeadCode = utils.getTaxHeadCodeFromOffenceTypeName(requestInfo, tenantId, offenceTypeName);
			log.info("Tax Head Code from OffenceType: {}", taxHeadCode);
		}
		
		// Calculate final amount (higher of the two)
		BigDecimal finalAmount = userAmount.max(mdmsRate);
		log.info("Final amount (higher of user amount and MDMS rate): {}", finalAmount);
		
		log.info("=== Dynamic Calculation Result ===");
		log.info("Tax Head Code: {}", taxHeadCode);
		log.info("Final Amount: {}", finalAmount);
		log.info("===============================================");
	}

	/**
	 * Test method to demonstrate minimumPayableAmount functionality
	 * This method shows how the total amount is calculated and stored in minimumPayableAmount
	 */
	public void testMinimumPayableAmount(RequestInfo requestInfo, String tenantId) {
		log.info("=== Testing Minimum Payable Amount Functionality ===");
		
		// Example calculation with your payload
		String offenceTypeName = "Loud Music After 10 PM";
		String challanAmount = "200";
		
		// Get offence type ID and rate
		String offenceTypeId = utils.mapOffenceTypeNameToId(requestInfo, tenantId, offenceTypeName);
		BigDecimal userAmount = new BigDecimal(challanAmount);
		BigDecimal mdmsRate = BigDecimal.ZERO;
		String taxHeadCode = "CH.CHALLAN_FINE";
		
		if (offenceTypeId != null) {
			mdmsRate = utils.getRateFromMDMS(requestInfo, tenantId, offenceTypeId);
			taxHeadCode = utils.getTaxHeadCodeFromOffenceTypeName(requestInfo, tenantId, offenceTypeName);
		}
		
		// Calculate final amount (higher of the two)
		BigDecimal finalAmount = userAmount.max(mdmsRate);
		
		// Simulate round-off calculation
		BigDecimal decimalValue = finalAmount.remainder(BigDecimal.ONE);
		BigDecimal roundOff = BigDecimal.ZERO;
		
		if (decimalValue.compareTo(new BigDecimal("0.5")) >= 0) {
			roundOff = BigDecimal.ONE.subtract(decimalValue);
		} else {
			roundOff = decimalValue.negate();
		}
		
		BigDecimal totalWithRoundOff = finalAmount.add(roundOff);
		
		log.info("User Amount: {}", userAmount);
		log.info("MDMS Rate: {}", mdmsRate);
		log.info("Final Amount (before round-off): {}", finalAmount);
		log.info("Round-off Amount: {}", roundOff);
		log.info("Total Amount (minimumPayableAmount): {}", totalWithRoundOff);
		
		log.info("=== Demand Creation ===");
		log.info("Demand will be created with:");
		log.info("- Tax Head: {}", taxHeadCode);
		log.info("- Amount: {}", finalAmount);
		log.info("- Round-off: {}", roundOff);
		log.info("- minimumPayableAmount: {}", totalWithRoundOff);
		log.info("===============================================");
	}

	public void cancelBill(RequestInfo requestInfo, Challan challan){
		Map<String, Object> request = new HashMap<>();
		Map<String, Object> updateBillCriteria = new HashMap<>();
		List<String> consumerCodes = Arrays.asList(challan.getChallanNo());
		String businessService = challan.getBusinessService();

		updateBillCriteria.put("tenantId", challan.getTenantId());
		updateBillCriteria.put("consumerCodes", consumerCodes);
		updateBillCriteria.put("businessService", businessService);
		updateBillCriteria.put("additionalDetails", challan.getAdditionalDetail());

		request.put("RequestInfo", requestInfo);
		request.put("UpdateBillCriteria", updateBillCriteria);

		StringBuilder url = new StringBuilder();
		url.append(config.getBillingHost()).append(config.getCancelBillEndpoint());
		try {
			restTemplate.postForObject(url.toString(), request, Map.class);
		}catch(Exception e) {
			log.error("Exception while fetching user: ", e);
		}
	}

	/**
	 * Update calculation with fee waiver
	 * Validates demandId, applies fee waiver by adding negative demand detail, and updates demand
	 * @param request CalculationReq with demandId and fee waiver information
	 * @return List of updated calculations
	 */
	public List<Calculation> updateCalculation(CalculationReq request) {
		// Validate demandId is present
		for(CalulationCriteria criteria : request.getCalulationCriteria()) {
			if(criteria.getDemandId() == null || criteria.getDemandId().isEmpty()) {
				throw new org.egov.tracer.model.CustomException("INVALID_REQUEST", 
					"demandId is required for update calculation");
			}
		}
		
		// Update demand with fee waiver - this will add negative demand detail
		List<Calculation> calculations = new LinkedList<>();
		for(CalulationCriteria criteria : request.getCalulationCriteria()) {
			Challan challan = criteria.getChallan();
			if (criteria.getChallan() == null && criteria.getChallanNo() != null) {
				challan = utils.getChallan(request.getRequestInfo(), criteria.getChallanNo(), criteria.getTenantId());
				criteria.setChallan(challan);
			}
			
			String businessService = challan.getBusinessService();
			// Update demand and get updated demand with new amounts
			org.egov.echallancalculation.web.models.demand.Demand updatedDemand = 
				demandService.updateDemandWithFeeWaiver(
					request.getRequestInfo(), 
					challan,
					businessService,
					criteria.getDemandId()
				);
			
			// Create tax head estimates from updated demand details
			List<TaxHeadEstimate> taxHeadEstimates = createTaxHeadEstimatesFromDemand(
				updatedDemand, 
				businessService
			);
			
			// Create calculation response with updated amounts
			Calculation calculation = new Calculation();
			calculation.setChallan(challan);
			calculation.setTenantId(criteria.getTenantId());
			calculation.setTaxHeadEstimates(taxHeadEstimates);
			calculations.add(calculation);
		}
		
		return calculations;
	}

	/**
	 * Create tax head estimates from updated demand details
	 * Groups demand details by tax head code and sums the amounts
	 * @param demand The updated demand object
	 * @param businessService The business service
	 * @return List of tax head estimates with updated amounts
	 */
	private List<TaxHeadEstimate> createTaxHeadEstimatesFromDemand(
			org.egov.echallancalculation.web.models.demand.Demand demand, 
			String businessService) {
		List<TaxHeadEstimate> estimates = new LinkedList<>();
		
		if(demand.getDemandDetails() == null || demand.getDemandDetails().isEmpty()) {
			return estimates;
		}
		
		// Group demand details by tax head code and sum amounts
		Map<String, BigDecimal> taxHeadAmountMap = new HashMap<>();
		
		String roundOffTaxHead = businessService + "_ROUNDOFF";
		
		for(org.egov.echallancalculation.web.models.demand.DemandDetail detail : demand.getDemandDetails()) {
			if(detail.getTaxHeadMasterCode() != null && detail.getTaxAmount() != null) {
				// Skip round-off tax head in main estimates (it's added separately)
				if(!detail.getTaxHeadMasterCode().equalsIgnoreCase(roundOffTaxHead)) {
					String taxHeadCode = detail.getTaxHeadMasterCode();
					BigDecimal currentAmount = taxHeadAmountMap.getOrDefault(taxHeadCode, BigDecimal.ZERO);
					taxHeadAmountMap.put(taxHeadCode, currentAmount.add(detail.getTaxAmount()));
				}
			}
		}
		
		// Create tax head estimates from grouped amounts
		for(Map.Entry<String, BigDecimal> entry : taxHeadAmountMap.entrySet()) {
			// Only include tax heads with non-zero amounts
			if(entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
				TaxHeadEstimate estimate = new TaxHeadEstimate();
				estimate.setTaxHeadCode(entry.getKey());
				estimate.setEstimateAmount(entry.getValue());
				estimates.add(estimate);
			}
		}
		
		// Add round-off separately if exists
		for(org.egov.echallancalculation.web.models.demand.DemandDetail detail : demand.getDemandDetails()) {
			if(detail.getTaxHeadMasterCode() != null && 
			   detail.getTaxHeadMasterCode().equalsIgnoreCase(roundOffTaxHead) &&
			   detail.getTaxAmount() != null &&
			   detail.getTaxAmount().compareTo(BigDecimal.ZERO) != 0) {
				TaxHeadEstimate roundOffEstimate = new TaxHeadEstimate();
				roundOffEstimate.setTaxHeadCode(detail.getTaxHeadMasterCode());
				roundOffEstimate.setEstimateAmount(detail.getTaxAmount());
				estimates.add(roundOffEstimate);
			}
		}
		
		return estimates;
	}

	
}
