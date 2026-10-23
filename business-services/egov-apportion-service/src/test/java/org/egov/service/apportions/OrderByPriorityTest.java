package org.egov.service.apportions;

import org.egov.config.ApportionConfig;
import org.egov.service.TaxHeadMasterService;
import org.egov.tracer.model.CustomException;
import org.egov.web.models.ApportionRequestV2;
import org.egov.web.models.Bucket;
import org.egov.web.models.TaxDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderByPriorityTest {

    @Mock
    private TaxHeadMasterService taxHeadMasterService;

    @Mock
    private ApportionConfig config;

    @Test
    void apportionPaidAmount_zeroPayment_setsAdjustedAmounts() {
        Bucket bucket = Bucket.builder()
                .amount(BigDecimal.ZERO)
                .adjustedAmount(BigDecimal.ZERO)
                .build();
        List<Bucket> buckets = new ArrayList<>();
        buckets.add(bucket);
        TaxDetail taxDetail = TaxDetail.builder()
                .fromPeriod(1L)
                .amountToBePaid(BigDecimal.ZERO)
                .buckets(buckets)
                .build();
        List<TaxDetail> taxDetails = new ArrayList<>();
        taxDetails.add(taxDetail);
        ApportionRequestV2 request = ApportionRequestV2.builder()
                .amountPaid(BigDecimal.ZERO)
                .taxDetails(taxDetails)
                .build();

        OrderByPriority orderByPriority = new OrderByPriority(taxHeadMasterService, config);

        List<TaxDetail> result = orderByPriority.apportionPaidAmount(request, null);

        assertThat(result.get(0).getBuckets().get(0).getAdjustedAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void apportionPaidAmount_missingFromPeriod_throwsCustomException() {
        when(config.getApportionByValueAndOrder()).thenReturn(false);

        Bucket bucket = Bucket.builder()
                .amount(BigDecimal.TEN)
                .priority(1)
                .build();
        List<Bucket> buckets = new ArrayList<>();
        buckets.add(bucket);
        TaxDetail taxDetail = TaxDetail.builder()
                .buckets(buckets)
                .build();
        List<TaxDetail> taxDetails = new ArrayList<>();
        taxDetails.add(taxDetail);
        ApportionRequestV2 request = ApportionRequestV2.builder()
                .amountPaid(BigDecimal.TEN)
                .isAdvanceAllowed(false)
                .taxDetails(taxDetails)
                .build();

        OrderByPriority orderByPriority = new OrderByPriority(taxHeadMasterService, config);

        assertThatThrownBy(() -> orderByPriority.apportionPaidAmount(request, null))
                .isInstanceOf(CustomException.class);
    }
}
