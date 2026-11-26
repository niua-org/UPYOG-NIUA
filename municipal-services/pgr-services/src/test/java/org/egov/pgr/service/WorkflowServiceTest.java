package org.egov.pgr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pgr.config.PGRConfiguration;
import org.egov.pgr.repository.ServiceRequestRepository;
import org.egov.pgr.web.models.Address;
import org.egov.pgr.web.models.Document;
import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.egov.pgr.web.models.ServiceWrapper;
import org.egov.pgr.web.models.Workflow;
import org.egov.pgr.web.models.workflow.BusinessService;
import org.egov.pgr.web.models.workflow.BusinessServiceResponse;
import org.egov.pgr.web.models.workflow.ProcessInstance;
import org.egov.pgr.web.models.workflow.ProcessInstanceResponse;
import org.egov.pgr.web.models.workflow.State;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    @Mock private PGRConfiguration pgrConfiguration;
    @Mock private ServiceRequestRepository repository;
    @Mock private ObjectMapper mapper;

    @InjectMocks
    private WorkflowService workflowService;

    @BeforeEach
    void stubWfUrls() {
        when(pgrConfiguration.getWfHost()).thenReturn("http://wf");
        when(pgrConfiguration.getWfBusinessServiceSearchPath()).thenReturn("/business");
        when(pgrConfiguration.getWfTransitionPath()).thenReturn("/transition");
        when(pgrConfiguration.getWfProcessInstanceSearchPath()).thenReturn("/process");
    }

    @Test
    void getBusinessService_whenFound_returnsFirstService() {
        ServiceRequest request = sampleRequest();
        BusinessService businessService = BusinessService.builder().businessService("PGR").build();
        BusinessServiceResponse response = new BusinessServiceResponse();
        response.setBusinessServices(List.of(businessService));
        when(repository.fetchResult(any(), any())).thenReturn(new Object());
        when(mapper.convertValue(any(), eq(BusinessServiceResponse.class))).thenReturn(response);

        BusinessService result = workflowService.getBusinessService(request);

        assertThat(result.getBusinessService()).isEqualTo("PGR");
    }

    @Test
    void getBusinessService_whenEmpty_throwsBusinessServiceNotFound() {
        ServiceRequest request = sampleRequest();
        BusinessServiceResponse response = new BusinessServiceResponse();
        response.setBusinessServices(Collections.emptyList());
        when(repository.fetchResult(any(), any())).thenReturn(new Object());
        when(mapper.convertValue(any(), eq(BusinessServiceResponse.class))).thenReturn(response);

        assertThatThrownBy(() -> workflowService.getBusinessService(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("is not found");
    }

    @Test
    void updateWorkflowStatus_setsApplicationStatusOnService() {
        ServiceRequest request = sampleRequest();
        BusinessService businessService = BusinessService.builder().businessService("PGR").build();
        BusinessServiceResponse bsResponse = new BusinessServiceResponse();
        bsResponse.setBusinessServices(List.of(businessService));
        State state = State.builder().applicationStatus("RESOLVED").build();
        ProcessInstance processInstance = ProcessInstance.builder().state(state).build();
        ProcessInstanceResponse piResponse = new ProcessInstanceResponse();
        piResponse.setProcessInstances(List.of(processInstance));

        when(repository.fetchResult(any(), any())).thenReturn(new Object());
        when(mapper.convertValue(any(), eq(BusinessServiceResponse.class))).thenReturn(bsResponse);
        when(mapper.convertValue(any(), eq(ProcessInstanceResponse.class))).thenReturn(piResponse);

        String status = workflowService.updateWorkflowStatus(request);

        assertThat(status).isEqualTo("RESOLVED");
        assertThat(request.getService().getApplicationStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void enrichWorkflow_whenProcessInstancesMatch_attachesWorkflowToWrappers() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        ServiceWrapper wrapper = ServiceWrapper.builder()
                .service(Service.builder()
                        .tenantId("pg.citya")
                        .serviceRequestId("SR-1")
                        .build())
                .build();
        ProcessInstance processInstance = ProcessInstance.builder()
                .businessId("SR-1")
                .action("FORWARD")
                .comment("note")
                .assignes(List.of(User.builder().uuid("u1").build()))
                .build();
        ProcessInstanceResponse piResponse = new ProcessInstanceResponse();
        piResponse.setProcessInstances(List.of(processInstance));
        when(repository.fetchResult(any(), any())).thenReturn(new Object());
        when(mapper.convertValue(any(), eq(ProcessInstanceResponse.class))).thenReturn(piResponse);

        List<ServiceWrapper> enriched = workflowService.enrichWorkflow(requestInfo, List.of(wrapper));

        assertThat(enriched).hasSize(1);
        assertThat(enriched.get(0).getWorkflow().getAction()).isEqualTo("FORWARD");
        assertThat(enriched.get(0).getWorkflow().getAssignes()).containsExactly("u1");
    }

    @Test
    void enrichWorkflow_whenProcessInstanceCountMismatch_throwsWorkflowNotFound() {
        RequestInfo requestInfo = RequestInfo.builder().build();
        ServiceWrapper wrapper = ServiceWrapper.builder()
                .service(Service.builder().tenantId("pg.citya").serviceRequestId("SR-1").build())
                .build();
        ProcessInstanceResponse piResponse = new ProcessInstanceResponse();
        piResponse.setProcessInstances(Collections.emptyList());
        when(repository.fetchResult(any(), any())).thenReturn(new Object());
        when(mapper.convertValue(any(), eq(ProcessInstanceResponse.class))).thenReturn(piResponse);

        assertThatThrownBy(() -> workflowService.enrichWorkflow(requestInfo, List.of(wrapper)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("workflow object is not found");
    }

    @Test
    void getWorkflow_mapsProcessInstancesByBusinessId() {
        ProcessInstance processInstance = ProcessInstance.builder()
                .businessId("SR-9")
                .action("CLOSE")
                .comment("done")
                .documents(List.of(Document.builder().fileStoreId("fs-1").build()))
                .build();

        Map<String, Workflow> workflowMap = workflowService.getWorkflow(List.of(processInstance));

        assertThat(workflowMap).containsKey("SR-9");
        assertThat(workflowMap.get("SR-9").getAction()).isEqualTo("CLOSE");
        assertThat(workflowMap.get("SR-9").getComments()).isEqualTo("done");
    }

    private static ServiceRequest sampleRequest() {
        Service service = Service.builder()
                .tenantId("pg.citya")
                .serviceRequestId("SR-1")
                .address(Address.builder().build())
                .build();
        Workflow workflow = Workflow.builder()
                .action("APPLY")
                .comments("hello")
                .assignes(List.of("assignee-1"))
                .verificationDocuments(List.of(Document.builder().fileStoreId("doc-1").build()))
                .build();
        return ServiceRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("u1").build()).build())
                .service(service)
                .workflow(workflow)
                .build();
    }
}
