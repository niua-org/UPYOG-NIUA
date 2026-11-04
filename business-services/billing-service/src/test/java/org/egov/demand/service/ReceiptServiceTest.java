/*
 * Known production issues (not fixed in tests):
 * - updateDemandFromReceipt null/empty guard uses operator precedence such that a non-null
 *   request with an empty receipt list may still fall through incorrectly in some cases.
 */
package org.egov.demand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.model.AuditDetail;
import org.egov.demand.model.Bill;
import org.egov.demand.model.BillAccountDetail;
import org.egov.demand.model.BillDetail;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.web.contract.BillRequest;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.demand.web.contract.Receipt;
import org.egov.demand.web.contract.ReceiptRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private DemandService demandService;

    @InjectMocks
    private ReceiptService receiptService;

    private RequestInfo requestInfo;

    @BeforeEach
    void setUp() {
        requestInfo = RequestInfo.builder().apiId("billing-service").build();
    }

    @Test
    void updateDemandFromReceipt_nullRequest_doesNothing() {
        receiptService.updateDemandFromReceipt(null, BillDetail.StatusEnum.CREATED, false);
        verify(demandService, never()).getDemands(any(), any());
    }

    @Test
    void updateDemandFromReceipt_emptyReceiptList_doesNothing() {
        ReceiptRequest receiptRequest = ReceiptRequest.builder()
                .requestInfo(requestInfo)
                .receipt(Collections.emptyList())
                .build();
        receiptService.updateDemandFromReceipt(receiptRequest, BillDetail.StatusEnum.CREATED, false);
        verify(demandService, never()).getDemands(any(), any());
    }

    @Test
    void updateDemandFromReceipt_receiptWithEmptyBills_doesNothing() {
        Receipt receipt = Receipt.builder().bill(Collections.emptyList()).build();
        ReceiptRequest receiptRequest = ReceiptRequest.builder()
                .requestInfo(requestInfo)
                .receipt(List.of(receipt))
                .build();
        receiptService.updateDemandFromReceipt(receiptRequest, BillDetail.StatusEnum.CREATED, false);
        verify(demandService, never()).getDemands(any(), any());
    }

    @Test
    void updateDemandFromReceipt_validReceipt_updatesCollectionAndCallsAsyncUpdate() {
        String demandId = "demand-1";
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode("TL_TAX")
                                .taxAmount(BigDecimal.valueOf(100))
                                .collectionAmount(BigDecimal.ZERO)
                                .build()))
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillAccountDetail accountDetail = BillAccountDetail.builder()
                .taxHeadCode("TL_TAX")
                .amount(BigDecimal.valueOf(100))
                .adjustedAmount(BigDecimal.valueOf(40))
                .build();
        BillDetail billDetail = BillDetail.builder()
                .demandId(demandId)
                .billAccountDetails(List.of(accountDetail))
                .build();
        Bill bill = Bill.builder().tenantId("ap.kurnool").billDetails(List.of(billDetail)).build();
        Receipt receipt = Receipt.builder().bill(List.of(bill)).build();
        ReceiptRequest receiptRequest = ReceiptRequest.builder()
                .requestInfo(requestInfo)
                .receipt(List.of(receipt))
                .build();

        receiptService.updateDemandFromReceipt(receiptRequest, BillDetail.StatusEnum.CREATED, false);

        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("40");
        ArgumentCaptor<DemandRequest> captor = ArgumentCaptor.forClass(DemandRequest.class);
        verify(demandService).updateAsync(captor.capture(), any());
        assertThat(captor.getValue().getDemands()).containsExactly(demand);
    }

    @Test
    void updateDemandFromReceipt_cancellation_subtractsAdjustedAmount() {
        String demandId = "demand-2";
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode("TL_TAX")
                                .taxAmount(BigDecimal.valueOf(100))
                                .collectionAmount(BigDecimal.valueOf(50))
                                .build()))
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillAccountDetail accountDetail = BillAccountDetail.builder()
                .taxHeadCode("TL_TAX")
                .adjustedAmount(BigDecimal.valueOf(20))
                .build();
        BillDetail billDetail = BillDetail.builder()
                .demandId(demandId)
                .billAccountDetails(List.of(accountDetail))
                .build();
        Bill bill = Bill.builder().tenantId("ap.kurnool").billDetails(List.of(billDetail)).build();
        Receipt receipt = Receipt.builder().bill(List.of(bill)).build();
        ReceiptRequest receiptRequest = ReceiptRequest.builder()
                .requestInfo(requestInfo)
                .receipt(List.of(receipt))
                .build();

        receiptService.updateDemandFromReceipt(receiptRequest, BillDetail.StatusEnum.CANCELLED, true);

        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("30");
    }

    @Test
    void updateDemandFromBill_nullTaxHeadCode_skipsDetailUpdate() {
        String demandId = "demand-3";
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode("TL_TAX")
                                .taxAmount(BigDecimal.TEN)
                                .collectionAmount(BigDecimal.ZERO)
                                .build()))
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillAccountDetail accountDetail = BillAccountDetail.builder().taxHeadCode(null).adjustedAmount(BigDecimal.ONE).build();
        BillDetail billDetail = BillDetail.builder()
                .demandId(demandId)
                .billAccountDetails(List.of(accountDetail))
                .build();
        Bill bill = Bill.builder().tenantId("ap.kurnool").billDetails(List.of(billDetail)).build();
        BillRequest billRequest = BillRequest.builder().requestInfo(requestInfo).bills(List.of(bill)).build();

        receiptService.updateDemandFromBill(billRequest, Set.of(demandId), false);

        assertThat(demand.getDemandDetails()).hasSize(1);
        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("0");
    }

    @Test
    void updateDemandFromBill_emptyDemandDetails_throwsWhenSortingNullList() {
        String demandId = "demand-4";
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(new ArrayList<>())
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillAccountDetail accountDetail = BillAccountDetail.builder()
                .taxHeadCode("ADVANCE")
                .amount(BigDecimal.valueOf(25))
                .adjustedAmount(BigDecimal.valueOf(25))
                .build();
        BillDetail billDetail = BillDetail.builder()
                .demandId(demandId)
                .billAccountDetails(List.of(accountDetail))
                .build();
        Bill bill = Bill.builder().tenantId("ap.kurnool").billDetails(List.of(billDetail)).build();
        BillRequest billRequest = BillRequest.builder().requestInfo(requestInfo).bills(List.of(bill)).build();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> receiptService.updateDemandFromBill(billRequest, Set.of(demandId), false))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateDemandFromBill_cancellationWithZeroAdjustedAmount_leavesCollectionsUnchanged() {
        String demandId = "demand-6";
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(
                        DemandDetail.builder()
                                .taxHeadMasterCode("TL_TAX")
                                .taxAmount(BigDecimal.valueOf(100))
                                .collectionAmount(BigDecimal.valueOf(40))
                                .build()))
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillAccountDetail accountDetail = BillAccountDetail.builder()
                .taxHeadCode("TL_TAX")
                .adjustedAmount(BigDecimal.ZERO)
                .build();
        BillDetail billDetail = BillDetail.builder()
                .demandId(demandId)
                .billAccountDetails(List.of(accountDetail))
                .build();
        Bill bill = Bill.builder().tenantId("ap.kurnool").billDetails(List.of(billDetail)).build();
        BillRequest billRequest = BillRequest.builder().requestInfo(requestInfo).bills(List.of(bill)).build();

        receiptService.updateDemandFromBill(billRequest, Set.of(demandId), true);

        assertThat(demand.getDemandDetails().get(0).getCollectionAmount()).isEqualByComparingTo("40");
    }

    @Test
    void updateDemandFromBill_multipleDemandDetailsForTaxHead_distributesPayment() {
        String demandId = "demand-5";
        DemandDetail low = DemandDetail.builder()
                .taxHeadMasterCode("TL_TAX")
                .taxAmount(BigDecimal.valueOf(50))
                .collectionAmount(BigDecimal.ZERO)
                .build();
        DemandDetail high = DemandDetail.builder()
                .taxHeadMasterCode("TL_TAX")
                .taxAmount(BigDecimal.valueOf(100))
                .collectionAmount(BigDecimal.ZERO)
                .build();
        Demand demand = Demand.builder()
                .id(demandId)
                .tenantId("ap.kurnool")
                .demandDetails(List.of(high, low))
                .build();
        when(demandService.getDemands(any(), any())).thenReturn(List.of(demand));

        BillAccountDetail accountDetail = BillAccountDetail.builder()
                .taxHeadCode("TL_TAX")
                .adjustedAmount(BigDecimal.valueOf(120))
                .build();
        BillDetail billDetail = BillDetail.builder()
                .demandId(demandId)
                .billAccountDetails(List.of(accountDetail))
                .build();
        Bill bill = Bill.builder().tenantId("ap.kurnool").billDetails(List.of(billDetail)).build();
        BillRequest billRequest = BillRequest.builder().requestInfo(requestInfo).bills(List.of(bill)).build();

        receiptService.updateDemandFromBill(billRequest, new HashSet<>(Set.of(demandId)), false);

        assertThat(low.getCollectionAmount()).isEqualByComparingTo("50");
        assertThat(high.getCollectionAmount()).isEqualByComparingTo("70");
    }
}
