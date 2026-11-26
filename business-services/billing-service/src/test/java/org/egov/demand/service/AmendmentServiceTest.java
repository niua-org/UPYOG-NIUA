package org.egov.demand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.amendment.model.Amendment;
import org.egov.demand.amendment.model.AmendmentCriteria;
import org.egov.demand.amendment.model.AmendmentRequest;
import org.egov.demand.amendment.model.AmendmentUpdate;
import org.egov.demand.amendment.model.AmendmentUpdateRequest;
import org.egov.demand.amendment.model.State;
import org.egov.demand.amendment.model.enums.AmendmentStatus;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.model.AuditDetails;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.model.UpdateBillCriteria;
import org.egov.demand.repository.AmendmentRepository;
import org.egov.demand.repository.BillRepositoryV2;
import org.egov.demand.util.Util;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.demand.web.validator.AmendmentValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmendmentServiceTest {

    @Mock
    private Util util;

    @Mock
    private ApplicationProperties props;

    @Mock
    private DemandService demandService;

    @Mock
    private BillRepositoryV2 billRepositoryV2;

    @Mock
    private AmendmentValidator amendmentValidator;

    @Mock
    private AmendmentRepository amendmentRepository;

    @InjectMocks
    private AmendmentService amendmentService;

    @Test
    void search_withoutMobile_delegatesToRepository() {
        AmendmentCriteria criteria = AmendmentCriteria.builder().tenantId("ap.kurnool").build();
        List<Amendment> expected = List.of(Amendment.builder().amendmentId("a1").build());
        when(amendmentRepository.getAmendments(criteria)).thenReturn(expected);

        assertThat(amendmentService.search(criteria, new RequestInfo())).isSameAs(expected);
        verify(demandService, never()).getDemands(any(), any());
    }

    @Test
    void search_withMobileAndNoDemands_returnsEmpty() {
        AmendmentCriteria criteria = AmendmentCriteria.builder()
                .tenantId("ap.kurnool")
                .mobileNumber("9999999999")
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of());

        assertThat(amendmentService.search(criteria, new RequestInfo())).isEmpty();
        verify(amendmentRepository, never()).getAmendments(any());
    }

    @Test
    void search_withMobile_enrichesConsumerCodesFromDemands() {
        AmendmentCriteria criteria = AmendmentCriteria.builder()
                .tenantId("ap.kurnool")
                .mobileNumber("9999999999")
                .consumerCode(new HashSet<>())
                .build();
        Demand demand = Demand.builder().consumerCode("cc-1").build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));
        when(amendmentRepository.getAmendments(criteria)).thenReturn(List.of());

        amendmentService.search(criteria, new RequestInfo());

        assertThat(criteria.getConsumerCode()).contains("cc-1");
        verify(amendmentRepository).getAmendments(criteria);
    }

    @Test
    void create_withoutWorkflow_updatesDemandImmediately() {
        when(props.getIsAmendmentworkflowEnabed()).thenReturn(false);
        Amendment amendment = Amendment.builder()
                .tenantId("ap.kurnool")
                .consumerCode("cc-1")
                .businessService("TL")
                .demandDetails(new ArrayList<>(List.of(
                        DemandDetail.builder().taxHeadMasterCode("TL_TAX").taxAmount(BigDecimal.TEN).build())))
                .build();
        AmendmentRequest request = AmendmentRequest.builder()
                .requestInfo(new RequestInfo())
                .amendment(amendment)
                .build();

        Demand demand = Demand.builder()
                .id("dem-1")
                .tenantId("ap.kurnool")
                .consumerCode("cc-1")
                .businessService("TL")
                .demandDetails(new ArrayList<>())
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));
        when(util.getAuditDetail(any())).thenReturn(AuditDetails.builder().build());

        Amendment created = amendmentService.create(request);

        assertThat(created).isSameAs(amendment);
        verify(amendmentRepository).saveAmendment(request);
        verify(demandService).update(any(DemandRequest.class), any());
        verify(billRepositoryV2).updateBillStatus(any(UpdateBillCriteria.class));
    }

    @Test
    void create_withWorkflow_callsWorkflowAndSkipsImmediateDemandUpdate() {
        when(props.getIsAmendmentworkflowEnabed()).thenReturn(true);
        Amendment amendment = Amendment.builder()
                .workflow(new org.egov.demand.amendment.model.ProcessInstance())
                .build();
        AmendmentRequest request = AmendmentRequest.builder()
                .requestInfo(new RequestInfo())
                .amendment(amendment)
                .build();
        when(util.callWorkFlow(any(), any())).thenReturn(State.builder().applicationStatus("ACTIVE").build());

        amendmentService.create(request);

        verify(amendmentRepository).saveAmendment(request);
        verify(demandService, never()).update(any(), any());
        assertThat(amendment.getStatus()).isEqualTo(AmendmentStatus.ACTIVE);
    }

    @Test
    void updateAmendment_activeStatus_triggersDemandUpdate() {
        when(props.getIsAmendmentworkflowEnabed()).thenReturn(false);
        AmendmentUpdate update = AmendmentUpdate.builder()
                .status(AmendmentStatus.ACTIVE)
                .amendmentId("a1")
                .tenantId("ap.kurnool")
                .build();
        AmendmentUpdateRequest request = AmendmentUpdateRequest.builder()
                .requestInfo(new RequestInfo())
                .amendmentUpdate(update)
                .build();
        Amendment fromSearch = Amendment.builder()
                .amendmentId("a1")
                .tenantId("ap.kurnool")
                .consumerCode("cc-1")
                .businessService("TL")
                .demandDetails(List.of())
                .build();
        when(amendmentValidator.validateAndEnrichAmendmentForUpdate(request)).thenReturn(fromSearch);
        when(amendmentRepository.getAmendments(any())).thenReturn(List.of(fromSearch));
        when(demandService.getDemands(any(), any())).thenReturn(List.of());

        amendmentService.updateAmendment(request);

        verify(amendmentRepository).updateAmendment(List.of(update));
    }

    @Test
    void updateDemandWithAmendmentTax_withMultipleDemands_picksLatestPeriod() {
        Demand older = Demand.builder()
                .id("old")
                .consumerCode("cc-1")
                .businessService("TL")
                .tenantId("ap.kurnool")
                .taxPeriodFrom(1L)
                .taxPeriodTo(2L)
                .demandDetails(new ArrayList<>())
                .build();
        Demand newer = Demand.builder()
                .id("new")
                .consumerCode("cc-1")
                .businessService("TL")
                .tenantId("ap.kurnool")
                .taxPeriodFrom(10L)
                .taxPeriodTo(20L)
                .demandDetails(new ArrayList<>())
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(new ArrayList<>(List.of(older, newer)));
        when(util.getAuditDetail(any())).thenReturn(AuditDetails.builder().build());

        Amendment amendment = Amendment.builder()
                .consumerCode("cc-1")
                .businessService("TL")
                .tenantId("ap.kurnool")
                .demandDetails(List.of(
                        DemandDetail.builder().taxHeadMasterCode("TL_TAX").taxAmount(BigDecimal.ONE).build()))
                .build();

        amendmentService.updateDemandWithAmendmentTax(new RequestInfo(), amendment);

        verify(demandService).update(any(DemandRequest.class), any());
        assertThat(newer.getDemandDetails()).hasSize(1);
    }
}
