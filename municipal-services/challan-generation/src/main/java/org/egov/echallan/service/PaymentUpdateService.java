package org.egov.echallan.service;


import java.util.HashMap;
import java.util.List;
import org.egov.common.contract.request.RequestInfo;
import org.egov.echallan.config.ChallanConfiguration;
import org.egov.echallan.enums.ChallanStatusEnum;
import org.egov.echallan.model.AuditDetails;
import org.egov.echallan.model.Challan;
import org.egov.echallan.model.Challan.StatusEnum;
import org.egov.echallan.model.ChallanRequest;
import org.egov.echallan.model.SearchCriteria;
import org.egov.echallan.producer.Producer;
import org.egov.echallan.util.ChallanConstants;
import org.egov.echallan.util.CommonUtils;
import org.egov.echallan.web.models.collection.PaymentDetail;
import org.egov.echallan.web.models.collection.PaymentRequest;
import org.egov.echallan.web.models.workflow.Workflow;
import org.egov.echallan.workflow.WorkflowIntegrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
 

@Service
@Slf4j
public class PaymentUpdateService {
	
	@Autowired
	private ObjectMapper mapper; 
	
	@Autowired
	private ChallanService challanService;
	
	@Autowired
	private Producer producer;
	
	@Autowired
	private ChallanConfiguration config;
	
	@Autowired
	 private CommonUtils commUtils;

	@Autowired(required = false)
	private WorkflowIntegrator workflowIntegrator;
	
	public void process(HashMap<String, Object> record) {

		try {
			log.info("Process for object"+ record);
			PaymentRequest paymentRequest = mapper.convertValue(record, PaymentRequest.class);
			RequestInfo requestInfo = paymentRequest.getRequestInfo();
			//Update the echallan only when the payment is fully done.
			if( paymentRequest.getPayment().getTotalAmountPaid().compareTo(paymentRequest.getPayment().getTotalDue())!=0) 
				return;
			List<PaymentDetail> paymentDetails = paymentRequest.getPayment().getPaymentDetails();
			for (PaymentDetail paymentDetail : paymentDetails) {
				SearchCriteria criteria = new SearchCriteria();
				criteria.setTenantId(paymentRequest.getPayment().getTenantId());
				criteria.setChallanNo(paymentDetail.getBill().getConsumerCode());
				criteria.setBusinessService(paymentDetail.getBusinessService());
				List<Challan> challans = challanService.search(criteria, requestInfo);
				//update echallan only if payment is done for echallan.
				if(!CollectionUtils.isEmpty(challans) ) {
					String uuid = requestInfo.getUserInfo().getUuid();
				    AuditDetails auditDetails = commUtils.getAuditDetails(uuid, true);
					for(Challan challan: challans){

						Workflow workflow=new Workflow();
						workflow.setAction(ChallanConstants.ACTION_PAY);
						challan.setWorkflow(workflow);

						String nextStatus = workflowIntegrator.transition(requestInfo,
								challan,
								challan.getWorkflow().getAction());

						challan.setApplicationStatus(StatusEnum.PAID);
						String status = String.valueOf(ChallanStatusEnum.CHALLAN_GENERATED);
						challan.setChallanStatus(nextStatus);
						challan.setReceiptNumber(paymentDetail.getReceiptNumber());
					}
					challans.get(0).setAuditDetails(auditDetails);
					ChallanRequest request = ChallanRequest.builder().requestInfo(requestInfo).challan(challans.get(0)).build();
					producer.push(config.getUpdateChallanTopic(), request);
				}
			}
		} catch (Exception e) {
			log.error("Exception while processing payment update: ",e);
		}

	}

}
