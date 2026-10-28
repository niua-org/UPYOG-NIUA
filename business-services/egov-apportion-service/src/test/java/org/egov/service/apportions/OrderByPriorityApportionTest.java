package org.egov.service.apportions;

import org.egov.config.ApportionConfig;
import org.egov.service.TaxHeadMasterService;
import org.egov.tracer.model.CustomException;
import org.egov.web.models.Bill;
import org.egov.web.models.BillAccountDetail;
import org.egov.web.models.BillDetail;
import org.egov.web.models.enums.Purpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderByPriorityApportionTest {

    @Mock
    private TaxHeadMasterService taxHeadMasterService;
    @Mock
    private ApportionConfig config;

    @Test
    void getBusinessService_returnsDefault() {
        OrderByPriorityApportion apportion = new OrderByPriorityApportion(taxHeadMasterService, config);
        assertThat(apportion.getBusinessService()).isEqualTo("DEFAULT");
    }

    @Test
    void apportionPaidAmount_zeroRemaining_setsZeroPaidOnDetails() {
        when(config.getApportionByValueAndOrder()).thenReturn(true);

        BillAccountDetail account = BillAccountDetail.builder()
                .amount(BigDecimal.ZERO)
                .taxHeadCode("T1")
                .order(1)
                .build();
        BillDetail billDetail = BillDetail.builder()
                .fromPeriod(1L)
                .amount(BigDecimal.ZERO)
                .billAccountDetails(Collections.singletonList(account))
                .build();
        Bill bill = Bill.builder()
                .amountPaid(BigDecimal.ZERO)
                .isAdvanceAllowed(false)
                .billDetails(Collections.singletonList(billDetail))
                .build();

        OrderByPriorityApportion apportion = new OrderByPriorityApportion(taxHeadMasterService, config);
        List<BillDetail> result = apportion.apportionPaidAmount(bill, null);

        assertThat(result.get(0).getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void apportionPaidAmount_partialPayment_setsAdjustedAmounts() {
        when(config.getApportionByValueAndOrder()).thenReturn(true);

        BillAccountDetail tax = BillAccountDetail.builder()
                .amount(new BigDecimal("100"))
                .taxHeadCode("TAX")
                .order(2)
                .build();
        BillDetail billDetail = BillDetail.builder()
                .fromPeriod(2L)
                .toPeriod(3L)
                .amount(new BigDecimal("100"))
                .billAccountDetails(new ArrayList<>(List.of(tax)))
                .build();
        Bill bill = Bill.builder()
                .amountPaid(new BigDecimal("40"))
                .isAdvanceAllowed(false)
                .businessService("PT")
                .billDetails(new ArrayList<>(List.of(billDetail)))
                .build();

        OrderByPriorityApportion apportion = new OrderByPriorityApportion(taxHeadMasterService, config);
        List<BillDetail> result = apportion.apportionPaidAmount(bill, null);

        assertThat(tax.getAdjustedAmount()).isEqualByComparingTo("40");
        assertThat(result.get(0).getAmountPaid()).isEqualByComparingTo("40");
    }

    @Test
    void apportionPaidAmount_excessPayment_createsAdvanceLine() {
        when(config.getApportionByValueAndOrder()).thenReturn(true);
        when(taxHeadMasterService.getAdvanceTaxHead("PT", null)).thenReturn("ADVANCE_TAX");

        BillAccountDetail tax = BillAccountDetail.builder()
                .amount(new BigDecimal("50"))
                .taxHeadCode("TAX")
                .order(1)
                .build();
        BillDetail billDetail = BillDetail.builder()
                .fromPeriod(1L)
                .toPeriod(2L)
                .amount(new BigDecimal("50"))
                .billAccountDetails(new ArrayList<>(List.of(tax)))
                .build();
        Bill bill = Bill.builder()
                .amountPaid(new BigDecimal("80"))
                .isAdvanceAllowed(false)
                .businessService("PT")
                .billDetails(new ArrayList<>(List.of(billDetail)))
                .build();

        OrderByPriorityApportion apportion = new OrderByPriorityApportion(taxHeadMasterService, config);
        apportion.apportionPaidAmount(bill, null);

        BillAccountDetail advance = billDetail.getBillAccountDetails().stream()
                .filter(d -> Purpose.ADVANCE_AMOUNT.equals(d.getPurpose()))
                .findFirst()
                .orElseThrow();
        assertThat(advance.getTaxHeadCode()).isEqualTo("ADVANCE_TAX");
        assertThat(advance.getAmount()).isEqualByComparingTo("-30");
    }

    @Test
    void apportionPaidAmount_invalidOrder_throwsCustomException() {
        when(config.getApportionByValueAndOrder()).thenReturn(false);

        BillAccountDetail negative = BillAccountDetail.builder()
                .amount(new BigDecimal("-10"))
                .order(5)
                .build();
        BillAccountDetail positive = BillAccountDetail.builder()
                .amount(new BigDecimal("10"))
                .order(1)
                .build();
        BillDetail billDetail = BillDetail.builder()
                .fromPeriod(1L)
                .toPeriod(2L)
                .billAccountDetails(new ArrayList<>(List.of(negative, positive)))
                .build();
        Bill bill = Bill.builder()
                .amountPaid(BigDecimal.TEN)
                .isAdvanceAllowed(false)
                .billDetails(Collections.singletonList(billDetail))
                .build();

        OrderByPriorityApportion apportion = new OrderByPriorityApportion(taxHeadMasterService, config);

        assertThatThrownBy(() -> apportion.apportionPaidAmount(bill, null))
                .isInstanceOf(CustomException.class);
    }
}
