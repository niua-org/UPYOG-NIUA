package org.upyog.family.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.persistence.repository.ServiceRequestRepository;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.Workflow;
import org.upyog.family.web.contract.workflow.ProcessInstance;
import org.upyog.family.web.contract.workflow.ProcessInstanceRequest;
import org.upyog.family.web.contract.workflow.ProcessInstanceResponse;
import org.upyog.family.web.contract.workflow.State;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class WorkflowService {

    private final FamilyConfiguration configs;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ObjectMapper mapper;

    @Autowired
    public WorkflowService(FamilyConfiguration configs, ServiceRequestRepository serviceRequestRepository,
                           ObjectMapper mapper) {
        this.configs = configs;
        this.serviceRequestRepository = serviceRequestRepository;
        this.mapper = mapper;
    }

    public State updateWorkflowStatus(FamilyRequest familyRequest) {
        if (familyRequest == null || familyRequest.getFamily() == null) {
            throw new CustomException(FamilyConstants.ERROR_INVALID_REQUEST, FamilyConstants.MSG_FAMILY_WORKFLOW_REQ_NULL);
        }

        Family family = familyRequest.getFamily();
        Workflow workflow = family.getWorkflow();

        if (workflow == null) {
            log.info("[UPYOG-FAMILY-SERVICE] No workflow payload found for familyId: {}, skipping workflow transition", family.getFamilyId());
            return null;
        }

        ProcessInstance processInstance = getProcessInstanceForFamily(familyRequest);
        ProcessInstanceRequest workflowRequest = new ProcessInstanceRequest(familyRequest.getRequestInfo(), Collections.singletonList(processInstance));

        return callWorkFlow(workflowRequest);
    }

    private ProcessInstance getProcessInstanceForFamily(FamilyRequest familyRequest) {
        Family family = familyRequest.getFamily();
        Workflow workflow = family.getWorkflow();

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setBusinessId(family.getFamilyId());
        processInstance.setAction(workflow.getAction());
        processInstance.setModuleName(configs.getFamilyModuleName());
        processInstance.setTenantId(family.getTenantId());
        processInstance.setBusinessService(configs.getFamilyBusinessService());
        processInstance.setDocuments(workflow.getDocuments());
        processInstance.setComment(workflow.getComments());

        if (!CollectionUtils.isEmpty(workflow.getAssignes())) {
            List<User> users = workflow.getAssignes().stream().map(uuid -> {
                User user = new User();
                user.setUuid(uuid);
                return user;
            }).toList();

            processInstance.setAssignes(users);
        }

        return processInstance;
    }

    /**
     * Method to integrate with workflow
     *
     * takes the Family request as parameter constructs the workflow request
     * and sets the resultant status from wf-response
     */
    public State callWorkFlow(ProcessInstanceRequest workflowReq) {
        StringBuilder url = new StringBuilder(configs.getWfHost().concat(configs.getWfTransitionPath()));
        Object optional = serviceRequestRepository.fetchResult(url, workflowReq);
        ProcessInstanceResponse response = mapper.convertValue(optional, ProcessInstanceResponse.class);

        if (response == null || CollectionUtils.isEmpty(response.getProcessInstances())) {
            throw new CustomException(FamilyConstants.ERROR_WORKFLOW, FamilyConstants.MSG_WORKFLOW_EMPTY_PROCESS_INSTANCES);
        }

        return response.getProcessInstances().get(0).getState();
    }

    public List<ProcessInstance> getProcessInstances(RequestInfo requestInfo, String tenantId, String businessId) {
        StringBuilder url = new StringBuilder(configs.getWfHost().concat(configs.getWfProcessInstanceSearchPath()));
        url.append("?tenantId=").append(tenantId).append("&businessIds=").append(businessId);

        Object requestBody = Collections.singletonMap("RequestInfo", requestInfo);
        Object optional = serviceRequestRepository.fetchResult(url, requestBody);
        ProcessInstanceResponse response = mapper.convertValue(optional, ProcessInstanceResponse.class);

        return (response != null && response.getProcessInstances() != null)
                ? response.getProcessInstances()
                : Collections.emptyList();
    }
}
