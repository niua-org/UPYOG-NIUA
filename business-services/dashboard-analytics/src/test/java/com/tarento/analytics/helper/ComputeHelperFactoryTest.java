package com.tarento.analytics.helper;

import com.tarento.analytics.constant.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ComputeHelperFactoryTest {

    @Mock
    private TargetPerDateComputeHelper targetPerDateComputeHelper;

    @InjectMocks
    private ComputeHelperFactory factory;

    @Test
    void getInstance_responseDiffDates_returnsTargetPerDateHelper() {
        assertThat(factory.getInstance(Constants.PostAggregationTheories.RESPONSE_DIFF_DATES))
                .isSameAs(targetPerDateComputeHelper);
    }

    @Test
    void getInstance_unknownIntent_returnsNull() {
        assertThat(factory.getInstance("UNKNOWN_INTENT")).isNull();
    }
}
