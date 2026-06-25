package org.egov.echallan.service;

import java.math.BigDecimal;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.enums.ChallanStatusEnum;
import org.egov.echallan.model.Amount;
import org.egov.echallan.model.AuditDetails;
import org.egov.echallan.model.Challan;
import org.egov.echallan.model.ChallanRequest;
import org.egov.echallan.model.SearchCriteria;
import org.egov.echallan.repository.ChallanRepository;
import org.egov.echallan.util.ChallanConstants;
import org.egov.echallan.util.CommonUtils;
import org.egov.echallan.util.ResponseInfoFactory;
import org.egov.echallan.validator.ChallanValidator;
import org.egov.echallan.web.models.user.UserDetailResponse;
import org.egov.echallan.workflow.WorkflowIntegrator;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service
@Slf4j
public class ChallanService {

    @Autowired
    private EnrichmentService enrichmentService;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

    private UserService userService;
    
    private ChallanRepository repository;
    
    private CalculationService calculationService;
    
    private ChallanValidator validator;

    private CommonUtils utils;
    
    private ChallanConfiguration config;
    
    private org.egov.echallan.repository.ServiceRequestRepository serviceRequestRepository;

	@Autowired(required = false)
	private WorkflowIntegrator workflowIntegrator;
    
    @Autowired
    public ChallanService(EnrichmentService enrichmentService, UserService userService,ChallanRepository repository,CalculationService calculationService,
    		ChallanValidator validator, CommonUtils utils, ChallanConfiguration config,
    		org.egov.echallan.repository.ServiceRequestRepository serviceRequestRepository) {
        this.enrichmentService = enrichmentService;
        this.userService = userService;
        this.repository = repository;
        this.calculationService = calculationService;
        this.validator = validator;
        this.utils = utils;
        this.config = config;
        this.serviceRequestRepository = serviceRequestRepository;
    }
    
    
	/**
	 * Enriches the Request and pushes to the Queue
	 *
	 * @param request ChallanRequest containing list of challans to be created
	 * @return Challan successfully created
	 */
	public Challan create(ChallanRequest request) {
		Object mdmsData = utils.mDMSCall(request);
		
		// Set default business service for all challans
		Challan challan = request.getChallan();
		if (StringUtils.isBlank(challan.getBusinessService())) {
			challan.setBusinessService("Challan_Generation");
		}
		
		// Auto-populate tax period from MDMS if not provided
		if (challan.getTaxPeriodFrom() == null || challan.getTaxPeriodTo() == null) {
			Map<String, Object> taxPeriodDetails = utils.fetchTaxPeriodFromMDMS(mdmsData, challan.getBusinessService());
			
			if (!taxPeriodDetails.isEmpty()) {
				if (challan.getTaxPeriodFrom() == null) {
					challan.setTaxPeriodFrom((Long) taxPeriodDetails.get("fromDate"));
				}
				if (challan.getTaxPeriodTo() == null) {
					challan.setTaxPeriodTo((Long) taxPeriodDetails.get("toDate"));
				}
			}
		}
		
		// Auto-populate amount and taxHeadCode from MDMS based on offence type name
		// Both amount and taxHeadCode come from OffenceType (like the previous flow but from OffenceType)
		if (StringUtils.isNotBlank(challan.getOffenceTypeName())) {
			BigDecimal amountFromMDMS = utils.fetchAmountFromOffenceTypeName(mdmsData, challan.getOffenceTypeName());
			
			if (amountFromMDMS != null) {
				// Fetch taxHeadCode from OffenceType (maintaining previous flow pattern but from OffenceType)
				String taxHeadCode = "CH.CHALLAN_FINE"; // Default fallback
				String taxHeadCodeFromMDMS = utils.fetchTaxHeadCodeFromOffenceTypeName(mdmsData, challan.getOffenceTypeName());
				if (StringUtils.isNotBlank(taxHeadCodeFromMDMS)) {
					taxHeadCode = taxHeadCodeFromMDMS;
				}
				
				// Create amount object from MDMS data
				Amount amountObject = Amount.builder()
					.taxHeadCode(taxHeadCode)
					.amount(amountFromMDMS)
					.build();
				
				challan.setAmount(Arrays.asList(amountObject));
				
				// Set challanAmount to MDMS amount if user hasn't provided it
				if (challan.getChallanAmount() == null) {
					challan.setChallanAmount(amountFromMDMS);
				}
			}
		}
		
		// Validate after populating amount
		validator.validateFields(request, mdmsData);
		
		enrichmentService.enrichCreateRequest(request);
		userService.createUser(request);
		
		// Set accountId to citizen's UUID after user creation
		if (challan.getCitizen() != null && challan.getCitizen().getUuid() != null) {
			challan.setAccountId(challan.getCitizen().getUuid());
		}
		
		// Ensure additionalDetail is not null to prevent persister issues
		if (challan.getAdditionalDetail() == null) {
			challan.setAdditionalDetail(new HashMap<>());
		}
		
		// Copy amount array to additionalDetail so it can be retrieved in search
		if (challan.getAmount() != null && !challan.getAmount().isEmpty()) {
			try {
				// Ensure amount objects have taxHeadCode (set default if missing)
				for (Amount amount : challan.getAmount()) {
					if (StringUtils.isBlank(amount.getTaxHeadCode())) {
						amount.setTaxHeadCode("CH.CHALLAN_FINE"); // Default tax head code
					}
				}
				
				// Convert additionalDetail to Map if it's not already
				@SuppressWarnings("unchecked")
				Map<String, Object> additionalDetailMap;
				ObjectMapper mapper = new ObjectMapper();
				if (challan.getAdditionalDetail() instanceof Map) {
					additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
				} else {
					// If it's JsonNode or other type, convert to Map
					@SuppressWarnings("unchecked")
					Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
					additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
				}
				
				// Convert amount list to JSON-serializable format and add to additionalDetail
				List<Map<String, Object>> amountList = new ArrayList<>();
				for (Amount amount : challan.getAmount()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> amountMap = (Map<String, Object>) (Object) mapper.convertValue(amount, Map.class);
					amountList.add(amountMap);
				}
				additionalDetailMap.put("amount", amountList);
				challan.setAdditionalDetail(additionalDetailMap);
				
			} catch (Exception e) {
				log.warn("Failed to copy amount to additionalDetail for challan {}: {}", 
					challan.getChallanNo(), e.getMessage());
			}
		}
		
		// Extract latitude and longitude from documents and store in additionalDetail
		extractAndStoreLocationFromDocuments(challan);
		
		// Set initial challan status
		challan.setChallanStatus(ChallanStatusEnum.CHALLAN_CREATED.toString());
		
		// Call workflow on create to generate process instance (if workflow action provided)
		// Note: Workflow service may have cache - if it fails, challan still gets created
		boolean workflowEnabled = config.getIsExternalWorkFlowEnabled() != null 
			? config.getIsExternalWorkFlowEnabled() : true;
		
		if (workflowEnabled) {
			try {
				if (workflowIntegrator != null 
						&& challan.getWorkflow() != null 
						&& StringUtils.isNotBlank(challan.getWorkflow().getAction())) {
					
					log.info("Calling workflow for challan {} with action: {} (user role: {})", 
						challan.getChallanNo(), 
						challan.getWorkflow().getAction(),
						request.getRequestInfo().getUserInfo().getRoles().get(0).getCode());
					
					String nextStatus = workflowIntegrator.transition(
						request.getRequestInfo(),
						challan,
						challan.getWorkflow().getAction()
					);
					
					// Update status based on workflow response
					if (StringUtils.isNotBlank(nextStatus)) {
						challan.setChallanStatus(nextStatus);
						log.info("Workflow set status to: {}", nextStatus);
					} else {
						// If workflow didn't return status, set to CHALLAN_GENERATED
						log.warn("Workflow didn't return status. Setting to CHALLAN_GENERATED");
						challan.setChallanStatus(ChallanStatusEnum.CHALLAN_GENERATED.toString());
					}
				} else {
					// No workflow action provided, set to CHALLAN_GENERATED directly
					log.info("No workflow action provided, setting status to CHALLAN_GENERATED");
					challan.setChallanStatus(ChallanStatusEnum.CHALLAN_GENERATED.toString());
				}
			} catch (Exception ex) {
				log.error("Workflow transition on create failed for challan {} with error: {}. Setting status to CHALLAN_GENERATED and continuing.", 
					challan.getChallanNo(), ex.getMessage());
				// Set default status if workflow fails - challan creation continues
				challan.setChallanStatus(ChallanStatusEnum.CHALLAN_GENERATED.toString());
			}
		} else {
			log.info("Workflow is disabled, setting status to CHALLAN_GENERATED");
			challan.setChallanStatus(ChallanStatusEnum.CHALLAN_GENERATED.toString());
		}
		
		// Call calculation service if SUBMIT action was provided during create
		if (challan.getWorkflow() != null 
				&& StringUtils.isNotBlank(challan.getWorkflow().getAction())
				&& challan.getWorkflow().getAction().equalsIgnoreCase(ChallanConstants.SUBMIT)) {
			
			log.info("SUBMIT action detected during create, calling calculation service for challan: {}", 
				challan.getChallanNo());
			
			try {
				calculationService.addCalculation(request);
				// Store demandId in additionalDetail after calculation
				storeDemandIdFromBill(request);
				log.info("Calculation completed successfully for challan: {}", challan.getChallanNo());
			} catch (Exception ex) {
				log.error("Calculation service failed for challan {} with error: {}. Continuing with challan creation.", 
					challan.getChallanNo(), ex.getMessage());
				// Don't fail challan creation if calculation fails
			}
		}
		
		// Move feeWaiver and calculation from root to additionalDetail before saving
		moveFieldsToAdditionalDetail(challan);
		
		repository.save(request);
		return request.getChallan();
	}
	
	
	 public List<Challan> search(SearchCriteria criteria, RequestInfo requestInfo){
	        List<Challan> challans;
	        enrichmentService.enrichSearchCriteriaWithAccountId(requestInfo,criteria);
	         if(criteria.getMobileNumber()!=null){
	        	 challans = getChallansFromMobileNumber(criteria,requestInfo);
	         }
	         else {
	        	 challans = getChallansWithOwnerInfo(criteria,requestInfo);
	         }
	       return challans;
	    }
	 public List<Challan> getChallansFromMobileNumber(SearchCriteria criteria, RequestInfo requestInfo){
		 List<Challan> challans = new LinkedList<>();
	        UserDetailResponse userDetailResponse = userService.getUser(criteria,requestInfo);
	        if(CollectionUtils.isEmpty(userDetailResponse.getUser())){
	            return Collections.emptyList();
	        }
	        enrichmentService.enrichSearchCriteriaWithOwnerids(criteria,userDetailResponse);
	        challans = repository.getChallans(criteria);

	        if(CollectionUtils.isEmpty(challans)){
	            return Collections.emptyList();
	        }

	        criteria=enrichmentService.getChallanCriteriaFromIds(challans);
	        challans = getChallansWithOwnerInfo(criteria,requestInfo);
	        return challans;
	    }
	 
	 public List<Challan> getChallansWithOwnerInfo(SearchCriteria criteria,RequestInfo requestInfo){
	        List<Challan> challans = repository.getChallans(criteria);
	        if(challans.isEmpty())
	            return Collections.emptyList();
	        challans = enrichmentService.enrichChallanSearch(challans,criteria,requestInfo);
	        return challans;
	    }

	/**
	 * gets the total count for a search request
	 *
	 * @param criteria The echallan search criteria
	 * @param requestInfo requestInfo
	 */
	public int countForSearch(SearchCriteria criteria, RequestInfo requestInfo){
		enrichmentService.enrichSearchCriteriaWithAccountId(requestInfo,criteria);
		int count=0;

		if(criteria.getMobileNumber()!=null){
			count = getCountOfChallansFromMobileNumber(criteria,requestInfo);
		}
		else {
			count = getCountOfChallansWithOwnerInfo(criteria,requestInfo);
		}
		return count;
	}

	public int getCountOfChallansFromMobileNumber(SearchCriteria criteria, RequestInfo requestInfo){
		UserDetailResponse userDetailResponse = userService.getUser(criteria,requestInfo);
		if(CollectionUtils.isEmpty(userDetailResponse.getUser())){
			return 0;
		}
		enrichmentService.enrichSearchCriteriaWithOwnerids(criteria,userDetailResponse);

		int count = repository.getChallanSearchCount(criteria);
		return count;
	}

	public int getCountOfChallansWithOwnerInfo(SearchCriteria criteria,RequestInfo requestInfo){
		int count = repository.getChallanSearchCount(criteria);
		return count;
	}
	 public List<Challan> searchChallans(ChallanRequest request){
	        SearchCriteria criteria = new SearchCriteria();
	        List<String> ids = new LinkedList<>();
	        ids.add(request.getChallan().getId());

	        criteria.setTenantId(request.getChallan().getTenantId());
	        criteria.setIds(ids);
	        criteria.setBusinessService(request.getChallan().getBusinessService());

	        List<Challan> challans = repository.getChallans(criteria);

	        if(challans.isEmpty())
	            return Collections.emptyList();
	        challans = enrichmentService.enrichChallanSearch(challans,criteria,request.getRequestInfo());
	        return challans;
	    }
	 
	 public Challan update(ChallanRequest request) {
		 Object mdmsData = utils.mDMSCall(request);
		 validator.validateFields(request, mdmsData);
		List<Challan> searchResult = searchChallans(request);
		validator.validateUpdateRequest(request,searchResult);
		 
		 // Preserve original citizen data from database - prevent citizen updates
		 Challan challan = request.getChallan();
		 Challan existingChallan = null;
		 if (searchResult != null && !searchResult.isEmpty()) {
			 existingChallan = searchResult.get(0);
			 if (existingChallan.getCitizen() != null) {
				 // Preserve the original citizen data from database
				 challan.setCitizen(existingChallan.getCitizen());
				 log.info("Preserved original citizen data for challan: {}", challan.getChallanNo());
			 }
		 }
		 
		 AuditDetails existingAuditDetails = existingChallan != null ? existingChallan.getAuditDetails() : null;
		 enrichmentService.enrichUpdateRequest(request, existingAuditDetails);
		 
		 // Copy amount array to additionalDetail so it can be retrieved in search
		 if (challan.getAmount() != null && !challan.getAmount().isEmpty()) {
			 try {
				 // Ensure amount objects have taxHeadCode (set default if missing)
				 for (Amount amount : challan.getAmount()) {
					 if (StringUtils.isBlank(amount.getTaxHeadCode())) {
						 amount.setTaxHeadCode("CH.CHALLAN_FINE"); // Default tax head code
					 }
				 }
				 
				 // Convert additionalDetail to Map if it's not already
				 @SuppressWarnings("unchecked")
				 Map<String, Object> additionalDetailMap;
				 ObjectMapper mapper = new ObjectMapper();
				 if (challan.getAdditionalDetail() instanceof Map) {
					 additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
				 } else {
					 // If it's JsonNode or other type, convert to Map
					 @SuppressWarnings("unchecked")
					 Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
					 additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
				 }
				 
				 // Convert amount list to JSON-serializable format and add to additionalDetail
				 List<Map<String, Object>> amountList = new ArrayList<>();
				 for (Amount amount : challan.getAmount()) {
					 @SuppressWarnings("unchecked")
					 Map<String, Object> amountMap = (Map<String, Object>) (Object) mapper.convertValue(amount, Map.class);
					 amountList.add(amountMap);
				 }
				 additionalDetailMap.put("amount", amountList);
				 challan.setAdditionalDetail(additionalDetailMap);
				 
			 } catch (Exception e) {
				 log.warn("Failed to copy amount to additionalDetail for challan {}: {}", 
					 challan.getChallanNo(), e.getMessage());
			 }
		 }

		 // Extract latitude and longitude from documents and store in additionalDetail
		 extractAndStoreLocationFromDocuments(challan);

		 // If workflow action present, transition and set status from WF response

		 try {
			 if (workflowIntegrator != null
					 && request.getChallan().getWorkflow() != null
					 && StringUtils.isNotBlank(
					 request.getChallan().getWorkflow().getAction())) {

				 String action = request.getChallan().getWorkflow().getAction();
				 String nextStatus = workflowIntegrator.transition(request.getRequestInfo(),
						 request.getChallan(),
						 action);
				 
				 // Handle SUBMIT action - create demand
				 if(action.equalsIgnoreCase(ChallanConstants.SUBMIT)){
					 calculationService.addCalculation(request);
					 // Store demandId in additionalDetail after calculation
					 storeDemandIdFromBill(request);
				 }
				 
				 // Handle SETTLED action - update existing demand with fee waiver
				 if(action.equalsIgnoreCase(ChallanConstants.ACTION_SETTLED)){
					 handleSettledAction(request, searchResult);
				 }

				 if (StringUtils.isNotBlank(nextStatus)) {
					 request.getChallan().setChallanStatus(nextStatus);
				 }
			 }
		 } catch (Exception ex) {
			 log.error("Workflow transition on update failed for challan {}", request.getChallan().getChallanNo(), ex);
		 }

		 // Move feeWaiver and calculation from root to additionalDetail before saving
		 moveFieldsToAdditionalDetail(challan);

		 repository.update(request);
		 return request.getChallan();
		}

	public Map<String,Object>  getChallanCountResponse(RequestInfo requestInfo, String tenantId){
		 validator.validateChallanCountRequest(tenantId);

		 Map<String,Object> response = new HashMap<>();
		 Map<String,String> results = new HashMap<>();
		 ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);

		 response.put("ResponseInfo",responseInfo);
		 results	= repository.fetchChallanCount(tenantId);

		 if(CollectionUtils.isEmpty(results) || results.get("totalChallan").equalsIgnoreCase("0"))
			 throw new CustomException("NO_RECORDS","No records found for the tenantId: "+tenantId);

		 response.put("ChallanCount",results);
		 return  response;
	 }


	public Map<String, Integer> getDynamicData(String tenantId) {
		Map<String,Integer> dynamicData = repository.fetchDynamicData(tenantId);
		
		return dynamicData;
	}
	
	public int getChallanValidity() {
		return Integer.valueOf(config.getChallanValidity());
	}

	/**
	 * Extracts latitude and longitude from documents and stores them in additionalDetail
	 * 
	 * @param challan The challan object containing documents
	 */
	private void extractAndStoreLocationFromDocuments(Challan challan) {
		if (challan.getUploadedDocumentDetails() == null || challan.getUploadedDocumentDetails().isEmpty()) {
			return;
		}
		
		try {
			// Find first document with latitude and longitude
			Double latitude = null;
			Double longitude = null;
			
			for (org.egov.echallan.model.DocumentDetail document : challan.getUploadedDocumentDetails()) {
				if (document.getLatitude() != null && document.getLongitude() != null) {
					latitude = document.getLatitude();
					longitude = document.getLongitude();
					break; // Use first document with location data
				}
			}
			
			// If location found, store in additionalDetail
			if (latitude != null && longitude != null) {
				// Ensure additionalDetail is not null
				if (challan.getAdditionalDetail() == null) {
					challan.setAdditionalDetail(new HashMap<>());
				}
				
				// Convert additionalDetail to Map if it's not already
				@SuppressWarnings("unchecked")
				Map<String, Object> additionalDetailMap;
				ObjectMapper mapper = new ObjectMapper();
				if (challan.getAdditionalDetail() instanceof Map) {
					additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
				} else {
					// If it's JsonNode or other type, convert to Map
					@SuppressWarnings("unchecked")
					Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
					additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
				}
				
				// Store latitude and longitude in additionalDetail
				additionalDetailMap.put("latitude", latitude);
				additionalDetailMap.put("longitude", longitude);
				challan.setAdditionalDetail(additionalDetailMap);
				
				log.info("Stored latitude: {} and longitude: {} in additionalDetail for challan {}", 
					latitude, longitude, challan.getChallanNo());
			}
		} catch (Exception e) {
			log.warn("Failed to extract and store location from documents for challan {}: {}", 
				challan.getChallanNo(), e.getMessage());
		}
	}

	/**
	 * Handles the settled action by updating existing demand with fee waiver
	 * Calculator will find demand using consumerCode and update it
	 * 
	 * @param request ChallanRequest with settled action
	 * @param searchResult Existing challan from database
	 */
	private void handleSettledAction(ChallanRequest request, List<Challan> searchResult) {
		if (searchResult == null || searchResult.isEmpty()) {
			log.error("Cannot process settled action: Existing challan not found");
			throw new CustomException("CHALLAN_NOT_FOUND", "Existing challan not found for settlement");
		}
		
		Challan challan = request.getChallan();
		Challan existingChallan = searchResult.get(0);
		
		try {
			// 1. Extract fee waiver amount from request
			BigDecimal feeWaiverAmount = extractFeeWaiverFromRequest(challan);
			if (feeWaiverAmount == null || feeWaiverAmount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new CustomException("INVALID_FEE_WAIVER", "Fee waiver amount must be provided and greater than zero");
			}
			
			// 2. Get demandId if available (optional - calculator can find demand using consumerCode)
			String demandId = getDemandIdFromChallan(existingChallan, request.getRequestInfo());
			
			// 3. Store fee waiver in additionalDetail for calculator
			storeFeeWaiverInAdditionalDetail(challan, feeWaiverAmount);
			
			// 4. Call calculator service to update existing demand
			// Calculator will find demand using consumerCode (challanNo) and update it by subtracting fee waiver
			log.info("Updating demand for challan {} with fee waiver: {} (demandId: {})", 
				challan.getChallanNo(), feeWaiverAmount, demandId != null ? demandId : "will be found by consumerCode");
			
			calculationService.updateCalculation(request, demandId);
			
			log.info("Successfully updated demand for challan {} with fee waiver", 
				challan.getChallanNo());
				
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error processing settled action for challan {}: {}", 
				challan.getChallanNo(), e.getMessage(), e);
			throw new CustomException("SETTLED_ACTION_ERROR", 
				"Failed to process settled action: " + e.getMessage());
		}
	}
	
	/**
	 * Extracts fee waiver amount from challan - checks root field first, then additionalDetail
	 * 
	 * @param challan Challan object
	 * @return Fee waiver amount or null if not found
	 */
	private BigDecimal extractFeeWaiverFromRequest(Challan challan) {
		// First check root field
		if (challan.getFeeWaiver() != null) {
			return challan.getFeeWaiver();
		}
		
		// Fallback to additionalDetail
		if (challan.getAdditionalDetail() == null) {
			return null;
		}
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> additionalDetailMap;
			
			if (challan.getAdditionalDetail() instanceof Map) {
				additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
				additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
			}
			
			Object feeWaiverObj = additionalDetailMap.get("feeWaiver");
			if (feeWaiverObj == null) {
				return null;
			}
			
			if (feeWaiverObj instanceof BigDecimal) {
				return (BigDecimal) feeWaiverObj;
			} else if (feeWaiverObj instanceof Number) {
				return BigDecimal.valueOf(((Number) feeWaiverObj).doubleValue());
			} else if (feeWaiverObj instanceof String) {
				return new BigDecimal((String) feeWaiverObj);
			}
			
		} catch (Exception e) {
			log.warn("Failed to extract fee waiver from additionalDetail for challan {}: {}", 
				challan.getChallanNo(), e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Retrieves demand ID from existing challan's additionalDetail or bill
	 * 
	 * @param challan Existing challan from database
	 * @param requestInfo RequestInfo for bill service call
	 * @return Demand ID or null if not found
	 */
	private String getDemandIdFromChallan(Challan challan, RequestInfo requestInfo) {
		// First try to get from additionalDetail
		if (challan.getAdditionalDetail() != null) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				Map<String, Object> additionalDetailMap;
				
				if (challan.getAdditionalDetail() instanceof Map) {
					additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
				} else {
					@SuppressWarnings("unchecked")
					Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
					additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
				}
				
				Object demandIdObj = additionalDetailMap.get("demandId");
				if (demandIdObj != null && StringUtils.isNotBlank(demandIdObj.toString())) {
					return demandIdObj.toString();
				}
			} catch (Exception e) {
				log.warn("Failed to extract demandId from additionalDetail for challan {}: {}", 
					challan.getChallanNo(), e.getMessage());
			}
		}
		
		// Try to get from bill if not found in additionalDetail
		if (requestInfo != null) {
			try {
				String demandId = getDemandIdFromBill(challan, requestInfo);
				if (StringUtils.isNotBlank(demandId)) {
					// Store it in additionalDetail for future use
					storeDemandIdInAdditionalDetail(challan, demandId);
					return demandId;
				}
			} catch (Exception e) {
				log.warn("Failed to get demandId from bill for challan {}: {}", 
					challan.getChallanNo(), e.getMessage());
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves demand ID from bill service
	 * 
	 * @param challan Challan object
	 * @param requestInfo RequestInfo object
	 * @return Demand ID or null if not found
	 */
	private String getDemandIdFromBill(Challan challan, RequestInfo requestInfo) {
		try {
			StringBuilder billUri = new StringBuilder();
			billUri.append(config.getBillingHost());
			billUri.append(config.getFetchBillEndpoint());
			billUri.append("?tenantId=").append(challan.getTenantId());
			billUri.append("&consumerCode=").append(challan.getChallanNo());
			billUri.append("&businessService=").append(challan.getBusinessService());
			
			org.egov.echallan.model.RequestInfoWrapper requestInfoWrapper = 
				new org.egov.echallan.model.RequestInfoWrapper(requestInfo);
			
			Object billResponse = serviceRequestRepository.fetchResult(billUri, requestInfoWrapper);
			
			if (billResponse != null) {
				com.jayway.jsonpath.JsonPath jsonPath = com.jayway.jsonpath.JsonPath.compile("$.Bill[0].billDetails[0].demandId");
				Object demandIdObj = jsonPath.read(billResponse);
				if (demandIdObj != null) {
					return demandIdObj.toString();
				}
			}
		} catch (Exception e) {
			log.warn("Failed to fetch demandId from bill for challan {}: {}", 
				challan.getChallanNo(), e.getMessage());
		}
		return null;
	}
	
	/**
	 * Stores demandId in challan's additionalDetail
	 * 
	 * @param challan Challan object
	 * @param demandId Demand ID to store
	 */
	private void storeDemandIdInAdditionalDetail(Challan challan, String demandId) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> additionalDetailMap;
			
			if (challan.getAdditionalDetail() instanceof Map) {
				additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
				additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
			}
			
			additionalDetailMap.put("demandId", demandId);
			challan.setAdditionalDetail(additionalDetailMap);
		} catch (Exception e) {
			log.warn("Failed to store demandId in additionalDetail for challan {}: {}", 
				challan.getChallanNo(), e.getMessage());
		}
	}
	
	/**
	 * Stores demandId from bill after calculation is done
	 * 
	 * @param request ChallanRequest
	 */
	private void storeDemandIdFromBill(ChallanRequest request) {
		try {
			Challan challan = request.getChallan();
			String demandId = getDemandIdFromBill(challan, request.getRequestInfo());
			if (StringUtils.isNotBlank(demandId)) {
				storeDemandIdInAdditionalDetail(challan, demandId);
				log.info("Stored demandId {} for challan {}", demandId, challan.getChallanNo());
			} else {
				log.warn("Could not retrieve demandId from bill for challan {}", challan.getChallanNo());
			}
		} catch (Exception e) {
			log.warn("Failed to store demandId from bill for challan {}: {}", 
				request.getChallan().getChallanNo(), e.getMessage());
		}
	}
	
	/**
	 * Stores fee waiver in additionalDetail for calculator service
	 * 
	 * @param challan Challan object
	 * @param feeWaiverAmount Fee waiver amount
	 */
	private void storeFeeWaiverInAdditionalDetail(Challan challan, BigDecimal feeWaiverAmount) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> additionalDetailMap;
			
			if (challan.getAdditionalDetail() instanceof Map) {
				additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
				additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
			}
			
			// Store fee waiver for calculator
			additionalDetailMap.put("feeWaiver", feeWaiverAmount);
			challan.setAdditionalDetail(additionalDetailMap);
			
		} catch (Exception e) {
			log.warn("Failed to store fee waiver in additionalDetail for challan {}: {}", 
				challan.getChallanNo(), e.getMessage());
		}
	}
	
	/**
	 * Moves feeWaiver and calculation from root object to additionalDetail before saving to DB
	 * This ensures these fields are stored in additionalDetail JSONB column
	 * 
	 * @param challan Challan object
	 */
	private void moveFieldsToAdditionalDetail(Challan challan) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> additionalDetailMap;
			
			// Ensure additionalDetail is a Map
			if (challan.getAdditionalDetail() instanceof Map) {
				additionalDetailMap = (Map<String, Object>) challan.getAdditionalDetail();
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> convertedMap = mapper.convertValue(challan.getAdditionalDetail(), Map.class);
				additionalDetailMap = convertedMap != null ? convertedMap : new HashMap<>();
			}
			
			// Copy feeWaiver from root to additionalDetail if present (for DB storage)
			// Keep in root for API response, but also store in additionalDetail for persistence
			if (challan.getFeeWaiver() != null) {
				additionalDetailMap.put("feeWaiver", challan.getFeeWaiver());
			}
			
			// Copy calculation from root to additionalDetail if present (for DB storage)
			// Keep in root for API response, but also store in additionalDetail for persistence
			if (challan.getCalculation() != null) {
				// Convert calculation object to Map for JSON storage
				@SuppressWarnings("unchecked")
				Map<String, Object> calculationMap = mapper.convertValue(challan.getCalculation(), Map.class);
				additionalDetailMap.put("calculation", calculationMap);
			}
			
			challan.setAdditionalDetail(additionalDetailMap);
			
		} catch (Exception e) {
			log.warn("Failed to move fields to additionalDetail for challan {}: {}", 
				challan.getChallanNo(), e.getMessage());
		}
	}

	
}