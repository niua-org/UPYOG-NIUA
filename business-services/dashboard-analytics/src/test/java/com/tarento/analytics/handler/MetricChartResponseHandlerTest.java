package com.tarento.analytics.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.helper.ComputeHelperFactory;
import com.tarento.analytics.utils.ResponseRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricChartResponseHandlerTest {

    @Mock
    private ConfigurationLoader configurationLoader;

    @Mock
    private ComputeHelperFactory computeHelperFactory;

    @InjectMocks
    private MetricChartResponseHandler handler;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void translate_sumsAggregationValues() throws Exception {
        ObjectNode configRoot = mapper.createObjectNode();
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(IResponseHandler.CHART_NAME, "Total Collection");
        chartNode.put(IResponseHandler.ACTION, "sum");
        chartNode.put(IResponseHandler.VALUE_TYPE, "amount");
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.METRIC.toString());
        chartNode.set(IResponseHandler.AGGS_PATH, mapper.createArrayNode().add("Amount Collected"));
        configRoot.set("metricViz", chartNode);
        when(configurationLoader.get(IResponseHandler.API_CONFIG_JSON)).thenReturn(configRoot);

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        ObjectNode amount = mapper.createObjectNode().put(IResponseHandler.VALUE, 100);
        aggs.set("Amount Collected", amount);
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("metricViz");
        request.setModuleLevel("PT");
        request.setResponseRecorder(new ResponseRecorder());

        AggregateDto result = handler.translate(request, aggregations);

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getHeaderValue()).isEqualTo(100.0);
        assertThat(request.getResponseRecorder().get("metricViz", "PT")).isNotNull();
    }

    @Test
    void translate_percentageAction_computesRatio() throws Exception {
        ObjectNode configRoot = mapper.createObjectNode();
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(IResponseHandler.CHART_NAME, "Achievement");
        chartNode.put(IResponseHandler.ACTION, IResponseHandler.PERCENTAGE);
        chartNode.put(IResponseHandler.VALUE_TYPE, "percentage");
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.METRIC.toString());
        ArrayNode paths = mapper.createArrayNode().add("Achieved").add("Target");
        chartNode.set(IResponseHandler.AGGS_PATH, paths);
        configRoot.set("pctViz", chartNode);
        when(configurationLoader.get(IResponseHandler.API_CONFIG_JSON)).thenReturn(configRoot);

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        aggs.set("Achieved", mapper.createObjectNode().put(IResponseHandler.VALUE, 25));
        aggs.set("Target", mapper.createObjectNode().put(IResponseHandler.VALUE, 100));
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("pctViz");
        request.setResponseRecorder(new ResponseRecorder());

        AggregateDto result = handler.translate(request, aggregations);

        assertThat(result.getData().get(0).getHeaderValue()).isEqualTo(25.0);
    }

    @Test
    void translate_insightPrefix_loadsConfigWithoutPrefix() throws Exception {
        ObjectNode configRoot = mapper.createObjectNode();
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(IResponseHandler.CHART_NAME, "Insight Metric");
        chartNode.put(IResponseHandler.ACTION, "sum");
        chartNode.put(IResponseHandler.VALUE_TYPE, "number");
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.METRIC.toString());
        chartNode.set(IResponseHandler.AGGS_PATH, mapper.createArrayNode().add("Count"));
        configRoot.set("innerViz", chartNode);
        when(configurationLoader.get(IResponseHandler.API_CONFIG_JSON)).thenReturn(configRoot);

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        aggs.set("Count", mapper.createObjectNode().put(IResponseHandler.VALUE, 7));
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("iinnerViz");
        request.setResponseRecorder(new ResponseRecorder());

        AggregateDto result = handler.translate(request, aggregations);

        assertThat(result.getData().get(0).getHeaderValue()).isEqualTo(7.0);
    }
}
