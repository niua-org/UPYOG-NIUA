package org.egov.ndc.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.producer.Producer;
import org.egov.ndc.repository.NDCRepository;
import org.egov.ndc.repository.ServiceRequestRepository;
import org.egov.ndc.util.NDCUtil;
import org.egov.ndc.web.model.ndc.NdcApplicationSearchCriteria;
import org.egov.tracer.model.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NDCServiceTest {

    @Mock private NDCUtil ndcUtil;
    @Mock private UserService userService;
    @Mock private org.egov.ndc.workflow.WorkflowIntegrator workflowIntegrator;
    @Mock private NDCConfiguration ndcConfiguration;
    @Mock private NDCRepository ndcRepository;
    @Mock private ObjectMapper mapper;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private Producer producer;
    @Mock private EnrichmentService enrichmentService;

    @InjectMocks
    private NDCService ndcService;

    @Test
    void searchNdcApplications_whenTenantMissing_throws() {
        RequestInfo requestInfo = RequestInfo.builder().userInfo(User.builder().uuid("u1").build()).build();

        assertThatThrownBy(() -> ndcService.searchNdcApplications(new NdcApplicationSearchCriteria(), requestInfo))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Tenant ID");
    }

    @Test
    void getApplicationsWithOwnerInfo_whenRepositoryEmpty_returnsEmpty() {
        NdcApplicationSearchCriteria criteria = NdcApplicationSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(ndcRepository.fetchNdcApplications(criteria)).thenReturn(Collections.emptyList());

        assertThat(ndcService.getApplicationsWithOwnerInfo(criteria, RequestInfo.builder().build())).isEmpty();
    }
}
