package org.upyog.family.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;
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

/**
 * Service orchestrating workflow lifecycle transitions and process instance queries
 * for the Family module via {@code egov-workflow-v2}.
 */
@Service
@Slf4j
public class WorkflowService {

    private final FamilyConfiguration configs;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ObjectMapper mapper;

    /**
     * Constructs a new {@code WorkflowService} with required configuration and dependencies.
     *
     * @param configs                  Family module configuration
     * @param serviceRequestRepository Repository for external service communication
     * @param mapper                   Jackson object mapper
     */
    @Autowired
    public WorkflowService(FamilyConfiguration configs, ServiceRequestRepository serviceRequestRepository,
                           ObjectMapper mapper) {
        this.configs = configs;
        this.serviceRequestRepository = serviceRequestRepository;
        this.mapper = mapper;
    }

    /**
     * Transitions the workflow state for the specified family request.
     *
     * @param familyRequest the incoming family request containing family details and workflow action
     * @return the resulting {@link State} after transition, or {@code null} if no workflow block is provided
     * @throws CustomException if familyRequest or family is null, or if workflow transition fails
     */
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
     * Dispatches a workflow transition request to {@code egov-workflow-v2} and returns the resulting state.
     *
     * @param workflowReq the workflow transition request payload
     * @return resulting {@link State} of the process instance
     * @throws CustomException if response is empty or contains no process instances
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

    /**
     * Queries workflow process instances for a specific business ID and tenant.
     *
     * @param requestInfo authentication and request metadata
     * @param tenantId    tenant identifier
     * @param businessId  family identifier
     * @return list of matching {@link ProcessInstance} records, or an empty list if none found
     */
    public List<ProcessInstance> getProcessInstances(RequestInfo requestInfo, String tenantId, String businessId) {
        String url = UriComponentsBuilder.fromHttpUrl(configs.getWfHost())
                .path(configs.getWfProcessInstanceSearchPath())
                .queryParam("tenantId", tenantId)
                .queryParam("businessIds", businessId)
                .build()
                .toUriString();

        Object requestBody = Collections.singletonMap("RequestInfo", requestInfo);
        Object optional = serviceRequestRepository.fetchResult(new StringBuilder(url), requestBody);
        ProcessInstanceResponse response = mapper.convertValue(optional, ProcessInstanceResponse.class);

        return (response != null && response.getProcessInstances() != null)
                ? response.getProcessInstances()
                : Collections.emptyList();
    }
}
