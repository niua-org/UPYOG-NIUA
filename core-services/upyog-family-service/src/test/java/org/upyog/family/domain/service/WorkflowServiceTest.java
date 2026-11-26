package org.upyog.family.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.persistence.repository.ServiceRequestRepository;
import org.upyog.family.util.FamilyConstants;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.Workflow;
import org.upyog.family.web.contract.workflow.Document;
import org.upyog.family.web.contract.workflow.ProcessInstance;
import org.upyog.family.web.contract.workflow.ProcessInstanceRequest;
import org.upyog.family.web.contract.workflow.ProcessInstanceResponse;
import org.upyog.family.web.contract.workflow.State;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private FamilyConfiguration configs;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private ObjectMapper mapper;
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        workflowService = new WorkflowService(configs, serviceRequestRepository, mapper);
    }

    @Test
    @DisplayName("updateWorkflowStatus should throw exception when request or family is null")
    void updateWorkflowStatus_NullRequestOrFamily() {
        CustomException ex1 = assertThrows(CustomException.class, () -> workflowService.updateWorkflowStatus(null));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex1.getCode());
        assertEquals(FamilyConstants.MSG_FAMILY_WORKFLOW_REQ_NULL, ex1.getMessage());

        CustomException ex2 = assertThrows(CustomException.class, () -> workflowService.updateWorkflowStatus(FamilyRequest.builder().build()));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex2.getCode());
        assertEquals(FamilyConstants.MSG_FAMILY_WORKFLOW_REQ_NULL, ex2.getMessage());
    }

    @Test
    @DisplayName("updateWorkflowStatus should throw exception when workflow object is not present in payload")
    void updateWorkflowStatus_NoWorkflowObject_ThrowsException() {
        Family family = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .build();
        FamilyRequest request = FamilyRequest.builder()
                .family(family)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.updateWorkflowStatus(request));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
        assertEquals(FamilyConstants.MSG_WORKFLOW_DATA_EMPTY, ex.getMessage());
    }

    @Test
    @DisplayName("updateWorkflowStatus should throw exception when workflow action is blank in payload")
    void updateWorkflowStatus_BlankWorkflowAction_ThrowsException() {
        Family family = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .workflow(Workflow.builder().action("").build())
                .build();
        FamilyRequest request = FamilyRequest.builder()
                .family(family)
                .build();

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.updateWorkflowStatus(request));
        assertEquals(FamilyConstants.ERROR_INVALID_REQUEST, ex.getCode());
        assertEquals(FamilyConstants.MSG_WORKFLOW_DATA_EMPTY, ex.getMessage());
    }

    @Test
    @DisplayName("updateWorkflowStatus should successfully transition and return state")
    void updateWorkflowStatus_Success() {
        when(configs.getWfHost()).thenReturn("http://localhost:8280");
        when(configs.getWfTransitionPath()).thenReturn("/egov-workflow-v2/egov-wf/process/_transition");
        when(configs.getFamilyModuleName()).thenReturn("family-service");
        when(configs.getFamilyBusinessService()).thenReturn("family-service");

        Document doc = Document.builder()
                .documentType("AADHAAR")
                .fileStoreId("filestore-uuid")
                .build();

        Workflow workflow = Workflow.builder()
                .action(FamilyConstants.ACTION_APPLY)
                .comments("Submitting family application")
                .assignes(Collections.singletonList("assignee-uuid-1"))
                .documents(Collections.singletonList(doc))
                .build();

        Family family = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .workflow(workflow)
                .build();

        RequestInfo requestInfo = RequestInfo.builder()
                .userInfo(User.builder().uuid("citizen-uuid").build())
                .build();

        FamilyRequest request = FamilyRequest.builder()
                .family(family)
                .requestInfo(requestInfo)
                .build();

        State stateResponse = State.builder()
                .state(FamilyStatus.PENDING_FOR_APPROVAL.name())
                .applicationStatus(FamilyStatus.PENDING_FOR_APPROVAL.name())
                .build();

        ProcessInstance piResponse = ProcessInstance.builder()
                .businessId("FAM-2026-0001")
                .state(stateResponse)
                .build();

        ProcessInstanceResponse response = ProcessInstanceResponse.builder()
                .processInstances(Collections.singletonList(piResponse))
                .build();

        Map<String, Object> responseMap = mapper.convertValue(response, Map.class);
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any(ProcessInstanceRequest.class)))
                .thenReturn(responseMap);

        State result = workflowService.updateWorkflowStatus(request);

        assertNotNull(result);
        assertEquals(FamilyStatus.PENDING_FOR_APPROVAL.name(), result.getState());
        assertEquals(FamilyStatus.PENDING_FOR_APPROVAL.name(), result.getApplicationStatus());
        verify(serviceRequestRepository).fetchResult(any(StringBuilder.class), any(ProcessInstanceRequest.class));
    }

    @Test
    @DisplayName("updateWorkflowStatus should throw exception when response contains no process instances")
    void updateWorkflowStatus_EmptyResponse_ThrowsException() {
        when(configs.getWfHost()).thenReturn("http://localhost:8280");
        when(configs.getWfTransitionPath()).thenReturn("/egov-workflow-v2/egov-wf/process/_transition");
        when(configs.getFamilyModuleName()).thenReturn("family-service");
        when(configs.getFamilyBusinessService()).thenReturn("family-service");

        Family family = Family.builder()
                .familyId("FAM-2026-0001")
                .tenantId("pg.citya")
                .workflow(Workflow.builder().action(FamilyConstants.ACTION_APPLY).build())
                .build();

        FamilyRequest request = FamilyRequest.builder()
                .family(family)
                .requestInfo(RequestInfo.builder().build())
                .build();

        ProcessInstanceResponse emptyResponse = ProcessInstanceResponse.builder()
                .processInstances(Collections.emptyList())
                .build();

        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any(ProcessInstanceRequest.class)))
                .thenReturn(mapper.convertValue(emptyResponse, Map.class));

        CustomException ex = assertThrows(CustomException.class, () -> workflowService.updateWorkflowStatus(request));
        assertEquals(FamilyConstants.ERROR_WORKFLOW, ex.getCode());
        assertEquals(FamilyConstants.MSG_WORKFLOW_EMPTY_PROCESS_INSTANCES, ex.getMessage());
    }

    @Test
    @DisplayName("getProcessInstances should fetch workflow history by tenant and businessId")
    void getProcessInstances_Success() {
        when(configs.getWfHost()).thenReturn("http://localhost:8280");
        when(configs.getWfProcessInstanceSearchPath()).thenReturn("/egov-workflow-v2/egov-wf/process/_search");

        RequestInfo requestInfo = RequestInfo.builder().build();

        ProcessInstance pi = ProcessInstance.builder()
                .businessId("FAM-2026-0001")
                .action(FamilyConstants.ACTION_APPLY)
                .state(State.builder().state(FamilyStatus.PENDING_FOR_APPROVAL.name()).build())
                .build();

        ProcessInstanceResponse response = ProcessInstanceResponse.builder()
                .processInstances(Collections.singletonList(pi))
                .build();

        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenReturn(mapper.convertValue(response, Map.class));

        List<ProcessInstance> result = workflowService.getProcessInstances(requestInfo, "pg.citya", "FAM-2026-0001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("FAM-2026-0001", result.get(0).getBusinessId());
    }
}
