package com.tarento.analytics.handler;

import com.tarento.analytics.enums.ChartType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerFactoryTest {

    @Mock
    private MetricChartResponseHandler metricChartResponseHandler;
    @Mock
    private LineChartResponseHandler lineChartResponseHandler;
    @Mock
    private PieChartResponseHandler pieChartResponseHandler;
    @Mock
    private PerformanceChartResponeHandler performingBarChartResponeHandler;
    @Mock
    private TableChartResponseHandler tableChartResponseHandler;
    @Mock
    private AdvanceTableChartResponseHandler advanceTableChartResponseHandler;
    @Mock
    private TablePostResponseHandler tablePostResponseHandler;

    @InjectMocks
    private ResponseHandlerFactory factory;

    @Test
    void getInstance_metricChartType_returnsMetricHandler() {
        assertThat(factory.getInstance(ChartType.METRIC)).isSameAs(metricChartResponseHandler);
    }

    @Test
    void getInstance_lineChartType_returnsLineHandler() {
        assertThat(factory.getInstance(ChartType.LINE)).isSameAs(lineChartResponseHandler);
    }

    @Test
    void get_nonTableChartType_returnsNullPostHandler() {
        assertThat(factory.get(ChartType.PIE)).isNull();
    }

    @Test
    void get_tableChartType_returnsTablePostHandler() {
        assertThat(factory.get(ChartType.TABLE)).isSameAs(tablePostResponseHandler);
    }
}
