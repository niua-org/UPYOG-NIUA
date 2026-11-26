package org.egov.bpa.service;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.util.BPAUtil;
import org.egov.bpa.util.NotificationUtil;
import org.egov.bpa.validator.BPAValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.workflow.ActionValidator;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BPAServiceTest {

    @Mock private WorkflowIntegrator wfIntegrator;
    @Mock private EnrichmentService enrichmentService;
    @Mock private EDCRService edcrService;
    @Mock private BPARepository repository;
    @Mock private ActionValidator actionValidator;
    @Mock private BPAValidator bpaValidator;
    @Mock private BPAUtil util;
    @Mock private CalculationService calculationService;
    @Mock private WorkflowService workflowService;
    @Mock private NotificationUtil notificationUtil;
    @Mock private BPALandService landService;
    @Mock private OCService ocService;
    @Mock private UserService userService;
    @Mock private NocService nocService;
    @Mock private BPAConfiguration config;
    @Mock private MultiStateInstanceUtil centralInstanceUtil;
    @Mock private PreapprovedPlanService preapprovedPlanService;

    @InjectMocks
    private BPAService bpaService;

    private final RequestInfo requestInfo = RequestInfo.builder().build();

    @Test
    void getBPAFromCriteria_whenRepositoryEmpty_returnsEmptyList() {
        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").applicationNo("BPA-1").build();
        when(repository.getBPAData(criteria, null)).thenReturn(Collections.emptyList());

        assertThat(bpaService.getBPAFromCriteria(criteria, requestInfo, null)).isEmpty();
    }

    @Test
    void getBPAFromCriteria_whenRepositoryHasRows_returnsThem() {
        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").applicationNo("BPA-1").build();
        BPA row = BPA.builder().applicationNo("BPA-1").tenantId("pb.amritsar").build();
        when(repository.getBPAData(criteria, null)).thenReturn(List.of(row));

        assertThat(bpaService.getBPAFromCriteria(criteria, requestInfo, null)).containsExactly(row);
    }

    @Test
    void getBPAWithBPAId_queriesRepositoryById() {
        BPA bpa = BPA.builder().id("bpa-id").tenantId("pb.amritsar").build();
        BPARequest request = BPARequest.builder().requestInfo(requestInfo).BPA(bpa).build();
        when(repository.getBPAData(any(), eq(null))).thenReturn(List.of(bpa));

        List<BPA> result = bpaService.getBPAWithBPAId(request);

        assertThat(result).containsExactly(bpa);
        verify(repository).getBPAData(any(BPASearchCriteria.class), eq(null));
    }

    @Test
    void search_whenMobileSearchAndNoLandInfo_returnsEmpty() {
        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").mobileNumber("9999999999").build();
        when(landService.searchLandInfoToBPA(eq(requestInfo), any())).thenReturn(new java.util.ArrayList<>());

        assertThat(bpaService.search(criteria, requestInfo)).isEmpty();
        verify(bpaValidator).validateSearch(requestInfo, criteria);
    }

    @Test
    void getEdcrPdf_whenApprovalNumberMissing_throwsInvalidRequest() {
        BPARequest request = BPARequest.builder()
                .requestInfo(requestInfo)
                .BPA(BPA.builder().tenantId("pb.amritsar").build())
                .build();

        assertThatThrownBy(() -> bpaService.getEdcrPdf(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Approval Number is required");
    }

    @Test
    void update_whenApplicationIdMissing_throwsUpdateError() {
        BPARequest request = BPARequest.builder()
                .requestInfo(requestInfo)
                .BPA(BPA.builder().tenantId("pb.amritsar").build())
                .build();
        when(centralInstanceUtil.getStateLevelTenant("pb.amritsar")).thenReturn("pb");

        assertThatThrownBy(() -> bpaService.update(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Application Not found in the System");
    }
}
