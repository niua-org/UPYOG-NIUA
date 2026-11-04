package org.egov.demand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.model.AuditDetails;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandCriteria;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.model.PaymentBackUpdateAudit;
import org.egov.demand.model.UpdateBillCriteria;
import org.egov.demand.repository.AmendmentRepository;
import org.egov.demand.repository.BillRepositoryV2;
import org.egov.demand.repository.DemandRepository;
import org.egov.demand.repository.ServiceRequestRepository;
import org.egov.demand.util.DemandEnrichmentUtil;
import org.egov.demand.util.Util;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.demand.web.contract.DemandResponse;
import org.egov.demand.web.contract.User;
import org.egov.demand.web.contract.UserResponse;
import org.egov.demand.web.contract.factory.ResponseFactory;
import org.egov.demand.web.validator.DemandValidatorV1;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemandServiceTest {

    @Mock
    private DemandRepository demandRepository;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ResponseFactory responseInfoFactory;
    @Mock
    private DemandEnrichmentUtil demandEnrichmentUtil;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private AmendmentRepository amendmentRepository;
    @Mock
    private BillRepositoryV2 billRepoV2;
    @Mock
    private Util util;
    @Mock
    private DemandValidatorV1 demandValidatorV1;

    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private DemandService demandService;

    private RequestInfo requestInfo;

    @BeforeEach
    void wireMapper() throws Exception {
        var field = DemandService.class.getDeclaredField("mapper");
        field.setAccessible(true);
        field.set(demandService, mapper);
        requestInfo = RequestInfo.builder().apiId("billing").build();
        when(applicationProperties.getUserServiceHostName()).thenReturn("http://user/");
        when(applicationProperties.getUserServiceSearchPath()).thenReturn("_search");
    }

    @Test
    void save_delegatesToRepository() {
        DemandRequest request = DemandRequest.builder().build();
        demandService.save(request);
        verify(demandRepository).save(request);
    }

    @Test
    void update_delegatesToRepository() {
        DemandRequest request = DemandRequest.builder().build();
        PaymentBackUpdateAudit audit = PaymentBackUpdateAudit.builder().build();
        demandService.update(request, audit);
        verify(demandRepository).update(request, audit);
    }

    @Test
    void getDemands_invalidCriteria_propagatesValidationError() {
        DemandCriteria criteria = DemandCriteria.builder().build();
        doThrow(new CustomException("INVALID", "bad"))
                .when(demandValidatorV1)
                .validateDemandCriteria(criteria, requestInfo);

        assertThatThrownBy(() -> demandService.getDemands(criteria, requestInfo))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getDemands_byRepository_enrichesPayerWhenPresent() {
        DemandCriteria criteria = DemandCriteria.builder().tenantId("ap.kurnool").build();
        User payer = new User();
        payer.setUuid("uuid-1");
        Demand demand = Demand.builder().id("d1").payer(payer).build();
        when(demandRepository.getDemands(criteria)).thenReturn(List.of(demand));

        UserResponse userResponse = new UserResponse();
        userResponse.setUser(List.of(payer));
        when(serviceRequestRepository.fetchResult(any(), any()))
                .thenReturn(mapper.convertValue(userResponse, Map.class));
        when(demandEnrichmentUtil.enrichPayer(any(), any())).thenReturn(List.of(demand));

        assertThat(demandService.getDemands(criteria, requestInfo)).containsExactly(demand);
    }

    @Test
    void getDemands_byMobile_noUsersFound_returnsEmpty() {
        DemandCriteria criteria = DemandCriteria.builder()
                .tenantId("ap.kurnool")
                .mobileNumber("9999999999")
                .build();
        UserResponse empty = new UserResponse();
        empty.setUser(List.of());
        when(serviceRequestRepository.fetchResult(any(), any()))
                .thenReturn(mapper.convertValue(empty, Map.class));

        assertThat(demandService.getDemands(criteria, requestInfo)).isEmpty();
        verify(demandRepository, never()).getDemands(any());
    }

    @Test
    void getDemands_byMobile_resolvesPayerAndQueriesRepository() {
        DemandCriteria criteria = DemandCriteria.builder()
                .tenantId("ap.kurnool")
                .mobileNumber("9999999999")
                .build();
        User user = new User();
        user.setUuid("uuid-2");
        UserResponse userResponse = new UserResponse();
        userResponse.setUser(List.of(user));
        when(serviceRequestRepository.fetchResult(any(), any()))
                .thenReturn(mapper.convertValue(userResponse, Map.class));

        Demand demand = Demand.builder().id("d2").build();
        when(demandRepository.getDemands(criteria)).thenReturn(List.of(demand));
        when(demandEnrichmentUtil.enrichPayer(any(), any())).thenReturn(List.of(demand));

        List<Demand> result = demandService.getDemands(criteria, requestInfo);
        assertThat(result).containsExactly(demand);
        verify(demandRepository).getDemands(criteria);
        assertThat(criteria.getPayer()).contains("uuid-2");
    }

    @Test
    void create_withoutAdvance_persistsDemandsAndExpiresBills() {
        Demand demand = Demand.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode("cc-1")
                .demandDetails(new ArrayList<>(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode("TL_TAX")
                                .taxAmount(BigDecimal.TEN)
                                .build())))
                .build();
        DemandRequest request = DemandRequest.builder().requestInfo(requestInfo).demands(List.of(demand)).build();

        when(util.getMDMSData(any(), any())).thenReturn(JsonPath.parse("{}"));
        when(util.getAuditDetail(requestInfo)).thenReturn(AuditDetails.builder().build());
        when(util.getIsAdvanceAllowed(eq("TL"), any())).thenReturn(false);
        when(amendmentRepository.getAmendments(any())).thenReturn(List.of());
        when(responseInfoFactory.getResponseInfo(requestInfo, HttpStatus.CREATED)).thenReturn(null);

        DemandResponse response = demandService.create(request);

        assertThat(response.getDemands()).hasSize(1);
        assertThat(demand.getId()).isNotBlank();
        verify(demandRepository).save(any(DemandRequest.class));
        verify(billRepoV2).updateBillStatus(any(UpdateBillCriteria.class));
    }

    @Test
    void updateAsync_withPaymentAudit_marksBillsPaid() {
        DemandDetail existing = DemandDetail.builder()
                .id("detail-1")
                .taxHeadMasterCode("TL_TAX")
                .taxAmount(BigDecimal.TEN)
                .collectionAmount(BigDecimal.ONE)
                .build();
        Demand demand = Demand.builder()
                .id("dem-1")
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode("cc-1")
                .demandDetails(new ArrayList<>(List.of(existing)))
                .build();
        DemandRequest request = DemandRequest.builder().requestInfo(requestInfo).demands(List.of(demand)).build();
        PaymentBackUpdateAudit audit = PaymentBackUpdateAudit.builder().paymentId("p1").build();

        when(util.getMDMSData(any(), any())).thenReturn(JsonPath.parse("{}"));
        when(util.getAuditDetail(requestInfo)).thenReturn(AuditDetails.builder().build());
        when(responseInfoFactory.getResponseInfo(requestInfo, HttpStatus.CREATED)).thenReturn(null);

        demandService.updateAsync(request, audit);

        verify(demandRepository).update(request, audit);
        verify(billRepoV2).updateBillStatus(any(UpdateBillCriteria.class));
        verify(util).updateDemandPaymentStatus(demand, true);
    }

    @Test
    void updateAsync_withoutPaymentAudit_expiresBills() {
        Demand demand = Demand.builder()
                .id("dem-2")
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode("cc-2")
                .demandDetails(new ArrayList<>())
                .build();
        DemandRequest request = DemandRequest.builder().requestInfo(requestInfo).demands(List.of(demand)).build();

        when(util.getMDMSData(any(), any())).thenReturn(JsonPath.parse("{}"));
        when(util.getAuditDetail(requestInfo)).thenReturn(AuditDetails.builder().build());
        when(responseInfoFactory.getResponseInfo(requestInfo, HttpStatus.CREATED)).thenReturn(null);

        demandService.updateAsync(request, null);

        verify(util).updateDemandPaymentStatus(demand, false);
        verify(billRepoV2).updateBillStatus(any(UpdateBillCriteria.class));
    }

    @Test
    void updateAsync_newDemandWithoutId_generatesIds() {
        Demand demand = Demand.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode("cc-3")
                .demandDetails(new ArrayList<>(List.of(
                        DemandDetail.builder().taxHeadMasterCode("TL_TAX").taxAmount(BigDecimal.ONE).build())))
                .build();
        DemandRequest request = DemandRequest.builder().requestInfo(requestInfo).demands(List.of(demand)).build();

        when(util.getMDMSData(any(), any())).thenReturn(JsonPath.parse("{}"));
        when(util.getAuditDetail(requestInfo)).thenReturn(AuditDetails.builder().build());
        when(responseInfoFactory.getResponseInfo(requestInfo, HttpStatus.CREATED)).thenReturn(null);

        demandService.updateAsync(request, null);

        assertThat(demand.getId()).isNotBlank();
    }

    @Test
    void create_withAdvanceAllowedAndNoPriorDemand_createsWithoutApportionCall() {
        Demand demand = Demand.builder()
                .tenantId("ap.kurnool")
                .businessService("TL")
                .consumerCode("cc-adv")
                .demandDetails(new ArrayList<>(List.of(
                        DemandDetail.builder().taxHeadMasterCode("TL_TAX").taxAmount(BigDecimal.TEN).build())))
                .build();
        DemandRequest request = DemandRequest.builder().requestInfo(requestInfo).demands(List.of(demand)).build();

        when(util.getMDMSData(any(), any())).thenReturn(JsonPath.parse("{}"));
        when(util.getAuditDetail(requestInfo)).thenReturn(AuditDetails.builder().build());
        when(util.getIsAdvanceAllowed(eq("TL"), any())).thenReturn(true);
        when(amendmentRepository.getAmendments(any())).thenReturn(List.of());
        when(demandRepository.getDemands(any())).thenReturn(List.of());
        when(responseInfoFactory.getResponseInfo(requestInfo, HttpStatus.CREATED)).thenReturn(null);

        demandService.create(request);

        verify(demandRepository).save(any(DemandRequest.class));
        verify(serviceRequestRepository, never()).fetchResult(any(), any());
    }

    @Test
    void getDemands_withEmptyDemandsFromRepository_skipsEnrichment() {
        DemandCriteria criteria = DemandCriteria.builder().tenantId("ap.kurnool").build();
        when(demandRepository.getDemands(criteria)).thenReturn(List.of());

        assertThat(demandService.getDemands(criteria, requestInfo)).isEmpty();
        verify(demandEnrichmentUtil, never()).enrichPayer(any(), any());
    }
}
