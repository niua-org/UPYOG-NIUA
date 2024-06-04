package org.upyog.chb.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.upyog.chb.config.CommunityHallBookingConfiguration;
import org.upyog.chb.constants.CommunityHallBookingConstants;
import org.upyog.chb.repository.ServiceRequestRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MdmsUtil {

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private CommunityHallBookingConfiguration config;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

    private String masterName;

    @Value("${egov.mdms.module.name}")
    private String moduleName;


	/*
	 * public Integer fetchRegistrationChargesFromMdms(RequestInfo requestInfo,
	 * String tenantId) { StringBuilder uri = new StringBuilder();
	 * uri.append(mdmsHost).append(mdmsUrl); MdmsCriteriaReq mdmsCriteriaReq =
	 * getMdmsRequestForCategoryList(requestInfo, tenantId); Object response = new
	 * HashMap<>(); Integer rate = 0; try { response =
	 * restTemplate.postForObject(uri.toString(), mdmsCriteriaReq, Map.class); rate
	 * = JsonPath.read(response, "$.MdmsRes.VTR.RegistrationCharges.[0].amount");
	 * }catch(Exception e) {
	 * log.error("Exception occurred while fetching category lists from mdms: ",e);
	 * } //log.info(ulbToCategoryListMap.toString()); return rate; }
	 * 
	 * private MdmsCriteriaReq getMdmsRequestForCategoryList(RequestInfo
	 * requestInfo, String tenantId) { MasterDetail masterDetail = new
	 * MasterDetail(); masterDetail.setName(masterName); List<MasterDetail>
	 * masterDetailList = new ArrayList<>(); masterDetailList.add(masterDetail);
	 * 
	 * ModuleDetail moduleDetail = new ModuleDetail();
	 * moduleDetail.setMasterDetails(masterDetailList);
	 * moduleDetail.setModuleName(moduleName); List<ModuleDetail> moduleDetailList =
	 * new ArrayList<>(); moduleDetailList.add(moduleDetail);
	 * 
	 * MdmsCriteria mdmsCriteria = new MdmsCriteria();
	 * mdmsCriteria.setTenantId(tenantId.split("\\.")[0]);
	 * mdmsCriteria.setModuleDetails(moduleDetailList);
	 * 
	 * MdmsCriteriaReq mdmsCriteriaReq = new MdmsCriteriaReq();
	 * mdmsCriteriaReq.setMdmsCriteria(mdmsCriteria);
	 * mdmsCriteriaReq.setRequestInfo(requestInfo);
	 * 
	 * return mdmsCriteriaReq; }
	 */
    
    
    /**
   	 * makes mdms call with the given criteria and reutrn mdms data
   	 * @param requestInfo
   	 * @param tenantId
   	 * @return
   	 */
   	public Object mDMSCall(RequestInfo requestInfo, String tenantId) {
   		MdmsCriteriaReq mdmsCriteriaReq = getMDMSRequest(requestInfo, tenantId);
   		Object result = serviceRequestRepository.fetchResult(getMdmsSearchUrl(), mdmsCriteriaReq);
   		log.info("Master data fetched from MDMS : " + result);
   		return result;
   	}
   	
   	
   	/**
   	 * Returns the URL for MDMS search end point
   	 *
   	 * @return URL for MDMS search end point
   	 */
   	public StringBuilder getMdmsSearchUrl() {
   		return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsPath());
   	}
   	
   	/**
   	 * prepares the mdms request object
   	 * @param requestInfo
   	 * @param tenantId
   	 * @return
   	 */
   	public MdmsCriteriaReq getMDMSRequest(RequestInfo requestInfo, String tenantId) {
   		List<ModuleDetail> moduleRequest = getBPAModuleRequest();
   		
   		log.info("Module details data needs to be fetched from MDMS : " + moduleRequest);

   		List<ModuleDetail> moduleDetails = new LinkedList<>();
   		moduleDetails.addAll(moduleRequest);

   		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId).build();

   		MdmsCriteriaReq mdmsCriteriaReq = MdmsCriteriaReq.builder().mdmsCriteria(mdmsCriteria).requestInfo(requestInfo)
   				.build();
   		return mdmsCriteriaReq;
   	}
   	
   	/**
   	 * Creates request to search ApplicationType and etc from MDMS
   	 * 
   	 * @param requestInfo
   	 *            The requestInfo of the request
   	 * @param tenantId
   	 *            The tenantId of the CHB
   	 * @return request to search ApplicationType and etc from MDMS
   	 */
   	public List<ModuleDetail> getBPAModuleRequest() {

   		// master details for CHB module
   		List<MasterDetail> chbMasterDtls = new ArrayList<>();

   		// filter to only get code field from master data
   		final String filterCode = "$.[?(@.active==true)].code";

   		chbMasterDtls.add(MasterDetail.builder().name(CommunityHallBookingConstants.CHB_PURPOSE).filter(filterCode).build());
   		chbMasterDtls.add(MasterDetail.builder().name(CommunityHallBookingConstants.CHB_RESIDENT_TYPE).filter(filterCode).build());
   		chbMasterDtls.add(MasterDetail.builder().name(CommunityHallBookingConstants.CHB_SPECIAL_CATEGORY).filter(filterCode).build());

   		ModuleDetail moduleDetail = ModuleDetail.builder().masterDetails(chbMasterDtls)
   				.moduleName(CommunityHallBookingConstants.CHB_MODULE).build();

   		// master details for common-masters module
   		List<MasterDetail> commonMasterDetails = new ArrayList<>();
   		ModuleDetail commonMasterMDtl = ModuleDetail.builder().masterDetails(commonMasterDetails)
   				.moduleName(CommunityHallBookingConstants.COMMON_MASTERS_MODULE).build();

   		return Arrays.asList(moduleDetail, commonMasterMDtl);

   	}
}