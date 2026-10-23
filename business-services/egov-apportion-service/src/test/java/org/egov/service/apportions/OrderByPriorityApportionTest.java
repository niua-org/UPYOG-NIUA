package org.egov.service.apportions;

import org.egov.config.ApportionConfig;
import org.egov.service.TaxHeadMasterService;
import org.egov.web.models.Bill;
import org.egov.web.models.BillAccountDetail;
import org.egov.web.models.BillDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
}
