package org.egov.inbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.inbox.config.InboxConfiguration;
import org.egov.inbox.repository.ServiceRequestRepository;
import org.egov.inbox.web.model.workflow.BusinessService;
import org.egov.inbox.web.model.workflow.ProcessInstanceSearchCriteria;
import org.egov.inbox.web.model.workflow.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private InboxConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(config, serviceRequestRepository, new ObjectMapper());
    }

    @Test
    void getStatusIdToBusinessServiceMap_mapsEachStateUuidToBusinessService() {
        State state = State.builder().uuid("state-1").applicationStatus("OPEN").build();
        BusinessService businessService = BusinessService.builder()
                .businessService("PT")
                .states(List.of(state))
                .build();

        Map<String, String> result = workflowService.getStatusIdToBusinessServiceMap(List.of(businessService));

        assertThat(result).containsEntry("state-1", "PT");
    }

    @Test
    void getApplicationStatusIdToStatusMap_mapsUuidToApplicationStatus() {
        State state = State.builder().uuid("state-2").applicationStatus("APPROVED").build();
        BusinessService businessService = BusinessService.builder()
                .businessService("TL")
                .states(List.of(state))
                .build();

        Map<String, String> result = workflowService.getApplicationStatusIdToStatusMap(List.of(businessService));

        assertThat(result).containsEntry("state-2", "APPROVED");
    }

    @Test
    void getProcessCount_sumsCountsAcrossBusinessServices() {
        when(config.getWorkflowHost()).thenReturn("http://workflow/");
        when(config.getProcessCountPath()).thenReturn("process/_count");
        when(serviceRequestRepository.fetchIntResult(any(), any())).thenReturn(4);

        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setTenantId("pb.amritsar");
        criteria.setBusinessService(new ArrayList<>(List.of("PT", "TL")));

        Integer count = workflowService.getProcessCount("pb.amritsar", RequestInfo.builder().build(), criteria);

        assertThat(count).isEqualTo(8);
        assertThat(criteria.getBusinessService()).containsExactly("PT", "TL");
    }
}
