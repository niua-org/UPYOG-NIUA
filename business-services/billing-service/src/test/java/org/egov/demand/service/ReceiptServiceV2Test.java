/*
 * Known production issues (not fixed in tests):
 * - updateDemandFromReceipt uses redundant null check: billReq == null || billReq != null && ...
 */
package org.egov.demand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.model.BillAccountDetailV2;
import org.egov.demand.model.BillDetailV2;
import org.egov.demand.model.BillV2;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.model.PaymentBackUpdateAudit;
import org.egov.demand.util.Constants;
import org.egov.demand.util.Util;
import org.egov.demand.web.contract.BillRequestV2;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jayway.jsonpath.JsonPath;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReceiptServiceV2Test {

    @Mock
    private DemandService demandService;

    @Mock
    private Util util;

    @InjectMocks
    private ReceiptServiceV2 receiptServiceV2;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        requestInfo = RequestInfo.builder().apiId("billing-service").build();
        when(util.prepareMdMsRequest(anyString(), anyString(), any(), anyString(), any()))
                .thenReturn(MdmsCriteriaReq.builder().build());
        when(util.getAttributeValues(any(MdmsCriteriaReq.class)))
                .thenReturn(JsonPath.parse("{\"MdmsRes\":{\"BillingService\":{\"TaxHeadMaster\":[]}}}"));
        when(util.getValueFromAdditionalDetailsForKey(any(), any())).thenReturn("payment-1");
    }

    @Test
    void updateDemandFromReceipt_nullOrEmptyBills_doesNothing() {
        receiptServiceV2.updateDemandFromReceipt(null, false);
        receiptServiceV2.updateDemandFromReceipt(BillRequestV2.builder().bills(Collections.emptyList()).build(), false);
        verify(demandService, never()).getDemands(any(), any());
    }

    @Test
    void updateDemandFromBill_singleDetail_paymentIncreasesCollection() {
        String demandId = "d1";
        Demand demand = demandWithSingleDetail(demandId, "TL_TAX", "100", "0");
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillRequestV2 request = billRequest(demandId, billDetail("TL_TAX", "100", "30", "30"));
        receiptServiceV2.updateDemandFromBill(request, Set.of(demandId), false);

        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("30");
        verifyUpdateAsync(false);
    }

    @Test
    void updateDemandFromBill_cancellation_decreasesCollection() {
        String demandId = "d2";
        Demand demand = demandWithSingleDetail(demandId, "TL_TAX", "100", "80");
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillRequestV2 request = billRequest(demandId, billDetail("TL_TAX", "100", "20", "20"));
        receiptServiceV2.updateDemandFromBill(request, Set.of(demandId), true);

        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("60");
    }

    @Test
    void updateDemandFromBill_zeroAmountPaidWithNonZeroAmount_skipsUpdate() {
        String demandId = "d3";
        Demand demand = demandWithSingleDetail(demandId, "TL_TAX", "100", "0");
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillDetailV2 detail = BillDetailV2.builder()
                .demandId(demandId)
                .amount(BigDecimal.valueOf(50))
                .amountPaid(BigDecimal.ZERO)
                .billAccountDetails(List.of(
                        BillAccountDetailV2.builder()
                                .taxHeadCode("TL_TAX")
                                .amount(BigDecimal.valueOf(50))
                                .adjustedAmount(BigDecimal.TEN)
                                .build()))
                .build();
        BillRequestV2 request = billRequest(demandId, detail);

        receiptServiceV2.updateDemandFromBill(request, Set.of(demandId), false);

        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("0");
    }

    @Test
    void updateDemandFromBill_advanceTaxHead_adjustsTaxAmount() {
        String demandId = "d4";
        Demand demand = demandWithSingleDetail(demandId, "TL_ADVANCE", "100", "40");
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        String mdmsJson = """
                {"MdmsRes":{"BillingService":{"TaxHeadMaster":[{"category":"ADVANCE_COLLECTION","service":"TL","code":"TL_ADVANCE"}]}}}
                """;
        when(util.getAttributeValues(any(MdmsCriteriaReq.class))).thenReturn(JsonPath.parse(mdmsJson));

        BillRequestV2 request = billRequest(
                demandId,
                billDetail("TL_ADVANCE", "150", "50", "50"),
                "TL");
        receiptServiceV2.updateDemandFromBill(request, Set.of(demandId), false);

        assertThat(demand.getDemandDetails().get(0).getTaxAmount()).isEqualByComparingTo("190");
        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("90");
    }

    @Test
    void updateDemandFromBill_cancellationOnNegativeTaxDetail_zeroesCollection() {
        String demandId = "d7";
        DemandDetail negative = DemandDetail.builder()
                .taxHeadMasterCode("TL_REBATE")
                .taxAmount(BigDecimal.valueOf(-50))
                .collectionAmount(BigDecimal.valueOf(-20))
                .build();
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(negative))
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillRequestV2 request = billRequest(demandId, billDetail("TL_REBATE", "-50", "30", "10"));
        receiptServiceV2.updateDemandFromBill(request, Set.of(demandId), true);

        assertThat(negative.getCollectionAmount()).isEqualByComparingTo("-30");
    }

    @Test
    void updateDemandFromBill_multipleDemandDetails_distributesPayment() {
        String demandId = "d5";
        DemandDetail first = DemandDetail.builder()
                .taxHeadMasterCode("TL_TAX")
                .taxAmount(BigDecimal.valueOf(30))
                .collectionAmount(BigDecimal.ZERO)
                .build();
        DemandDetail second = DemandDetail.builder()
                .taxHeadMasterCode("TL_TAX")
                .taxAmount(BigDecimal.valueOf(100))
                .collectionAmount(BigDecimal.ZERO)
                .build();
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(second, first))
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillRequestV2 request = billRequest(demandId, billDetail("TL_TAX", "130", "80", "80"));
        receiptServiceV2.updateDemandFromBill(request, Set.of(demandId), false);

        assertThat(first.getCollectionAmount()).isEqualByComparingTo("30");
        assertThat(second.getCollectionAmount()).isEqualByComparingTo("50");
    }

    @Test
    void updateDemandFromReceipt_delegatesToUpdateDemandFromBill() {
        String demandId = "d6";
        Demand demand = demandWithSingleDetail(demandId, "TL_TAX", "50", "0");
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillDetailV2 detail = billDetail("TL_TAX", "50", "25", "25");
        detail.setDemandId(demandId);
        BillRequestV2 request = BillRequestV2.builder()
                .requestInfo(requestInfo)
                .bills(List.of(BillV2.builder()
                        .id("bill-1")
                        .tenantId("ap.kurnool")
                        .businessService("TL")
                        .status(BillV2.BillStatus.ACTIVE)
                        .additionalDetails(JsonNodeFactory.instance.objectNode().put(Constants.PAYMENT_ID_KEY, "p1"))
                        .billDetails(List.of(detail))
                        .build()))
                .build();

        receiptServiceV2.updateDemandFromReceipt(request, false);

        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("25");
        verifyUpdateAsync(false);
    }

    private void verifyUpdateAsync(boolean cancellation) {
        ArgumentCaptor<PaymentBackUpdateAudit> auditCaptor = ArgumentCaptor.forClass(PaymentBackUpdateAudit.class);
        ArgumentCaptor<DemandRequest> requestCaptor = ArgumentCaptor.forClass(DemandRequest.class);
        verify(demandService).updateAsync(requestCaptor.capture(), auditCaptor.capture());
        assertThat(auditCaptor.getValue().getIsReceiptCancellation()).isEqualTo(cancellation);
        assertThat(auditCaptor.getValue().getPaymentId()).isEqualTo("payment-1");
        assertThat(requestCaptor.getValue().getRequestInfo()).isSameAs(requestInfo);
    }

    private static Demand demandWithSingleDetail(String demandId, String taxHead, String tax, String collected) {
        return Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode(taxHead)
                                .taxAmount(new BigDecimal(tax))
                                .collectionAmount(new BigDecimal(collected))
                                .build()))
                .build();
    }

    private BillRequestV2 billRequest(String demandId, BillDetailV2 detail) {
        return billRequest(demandId, detail, "TL");
    }

    private BillRequestV2 billRequest(String demandId, BillDetailV2 detail, String businessService) {
        detail.setDemandId(demandId);
        return BillRequestV2.builder()
                .requestInfo(requestInfo)
                .bills(List.of(BillV2.builder()
                        .id("bill-id")
                        .tenantId("ap.kurnool")
                        .businessService(businessService)
                        .status(BillV2.BillStatus.ACTIVE)
                        .additionalDetails(JsonNodeFactory.instance.objectNode())
                        .billDetails(List.of(detail))
                        .build()))
                .build();
    }

    private static BillDetailV2 billDetail(String taxHead, String amount, String amountPaid, String adjusted) {
        return BillDetailV2.builder()
                .amount(new BigDecimal(amount))
                .amountPaid(new BigDecimal(amountPaid))
                .billAccountDetails(List.of(
                        BillAccountDetailV2.builder()
                                .taxHeadCode(taxHead)
                                .amount(new BigDecimal(amount))
                                .adjustedAmount(new BigDecimal(adjusted))
                                .build()))
                .build();
    }
}
