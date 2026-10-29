package com.tarento.analytics.helper;

import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.RequestDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TargetPerDateComputeHelperTest {

    private final TargetPerDateComputeHelper helper = new TargetPerDateComputeHelper();

    @Test
    void compute_withDateRange_scalesValue() {
        AggregateRequestDto request = new AggregateRequestDto();
        RequestDate dates = new RequestDate();
        dates.setStartDate("1585679400000");
        dates.setEndDate("1617215399000");
        request.setRequestDate(dates);

        Double result = helper.compute(request, 100.0);

        assertThat(result).isNotNull();
    }

    @Test
    void compute_withoutDates_returnsOriginalValue() {
        AggregateRequestDto request = new AggregateRequestDto();

        assertThat(helper.compute(request, 42.0)).isEqualTo(42.0);
    }
}
