package org.egov.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.config.ApportionConfig;
import org.egov.producer.Producer;
import org.egov.web.models.ApportionRequest;
import org.egov.web.models.ApportionRequestV2;
import org.egov.web.models.Bill;
import org.egov.web.models.BillAccountDetail;
import org.egov.web.models.BillDetail;
import org.egov.web.models.Bucket;
import org.egov.web.models.Demand;
import org.egov.web.models.DemandDetail;
import org.egov.web.models.TaxDetail;
import org.egov.web.models.enums.DemandApportionRequest;
import org.egov.web.models.enums.Purpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApportionServiceV2Test {

    @Mock
    private Producer producer;
    @Mock
    private ApportionConfig config;
    @Mock
    private MDMSService mdmsService;
    @Mock
    private TranslationService translationService;

    @Test
    void apportionBills_translatesAndApportions() {
        ApportionV2 apportion = mock(ApportionV2.class);
        when(apportion.getBusinessService()).thenReturn("DEFAULT");
        when(config.getBillRequestTopic()).thenReturn("req");
        when(config.getBillResponseTopic()).thenReturn("res");
        when(mdmsService.mDMSCall(any(), any())).thenReturn(new Object());

        BillAccountDetail account = BillAccountDetail.builder().id("a1").taxHeadCode("T1").build();
        BillDetail billDetail = BillDetail.builder().id("d1").fromPeriod(1L).billAccountDetails(Collections.singletonList(account)).build();
        Bill bill = Bill.builder().businessService("WS").amountPaid(BigDecimal.ONE).billDetails(Collections.singletonList(billDetail)).build();
        ApportionRequest request = ApportionRequest.builder()
                .requestInfo(new RequestInfo())
                .tenantId("pb")
                .bills(Collections.singletonList(bill))
                .build();

        TaxDetail taxDetail = TaxDetail.builder().entityId("d1").amountPaid(BigDecimal.ONE).build();
        taxDetail.addBucket(Bucket.builder().entityId("a1").adjustedAmount(BigDecimal.ONE).taxHeadCode("T1").build());
        when(translationService.translate(any(Bill.class))).thenReturn(
                ApportionRequestV2.builder().taxDetails(Collections.singletonList(taxDetail)).build());
        when(apportion.apportionPaidAmount(any(), any())).thenReturn(Collections.singletonList(taxDetail));

        ApportionServiceV2 service = new ApportionServiceV2(List.of(apportion), producer, config, mdmsService, translationService);
        assertThat(service.apportionBills(request)).hasSize(1);
    }

    @Test
    void apportionDemands_updatesCollectionAmounts() {
        ApportionV2 apportion = mock(ApportionV2.class);
        when(apportion.getBusinessService()).thenReturn("DEFAULT");
        when(config.getDemandRequestTopic()).thenReturn("d-req");
        when(config.getDemandResponseTopic()).thenReturn("d-res");
        when(mdmsService.mDMSCall(any(), any())).thenReturn(new Object());

        DemandDetail demandDetail = DemandDetail.builder().id("dd1").taxHeadMasterCode("T1").build();
        Demand demand = Demand.builder().id("d1").businessService("WS").taxPeriodFrom(1L)
                .demandDetails(Collections.singletonList(demandDetail)).build();
        DemandApportionRequest request = DemandApportionRequest.builder()
                .requestInfo(new RequestInfo())
                .tenantId("pb")
                .demands(Collections.singletonList(demand))
                .build();

        Bucket bucket = Bucket.builder().entityId("dd1").adjustedAmount(BigDecimal.TEN).build();
        TaxDetail taxDetail = TaxDetail.builder().entityId("d1").buckets(Collections.singletonList(bucket)).build();
        when(translationService.translate(any(List.class), any())).thenReturn(
                ApportionRequestV2.builder().businessService("WS").amountPaid(BigDecimal.ZERO)
                        .isAdvanceAllowed(false).taxDetails(Collections.emptyList()).build());
        when(apportion.apportionPaidAmount(any(), any())).thenReturn(Collections.singletonList(taxDetail));

        ApportionServiceV2 service = new ApportionServiceV2(List.of(apportion), producer, config, mdmsService, translationService);
        List<Demand> result = service.apportionDemands(request);

        assertThat(result.get(0).getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
