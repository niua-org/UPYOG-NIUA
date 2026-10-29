package com.tarento.analytics.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.dto.RequestDate;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.helper.ComputedFieldFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LineChartResponseHandlerTest {

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ComputedFieldFactory computedFieldFactory;

    @InjectMocks
    private LineChartResponseHandler handler;

    private ObjectNode chartNode;

    @BeforeEach
    void setUp() {
        chartNode = mapper.createObjectNode();
        chartNode.put("interval", "month");
        chartNode.put(IResponseHandler.VALUE_TYPE, "number");
        chartNode.put("isCumulative", false);
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.LINE.toString());
        ArrayNode paths = mapper.createArrayNode().add("Total Application");
        chartNode.set(IResponseHandler.AGGS_PATH, paths);
    }

    @Test
    void translate_buildsPlotsForMonthlyBuckets() throws Exception {
        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        ObjectNode series = mapper.createObjectNode();
        ArrayNode buckets = mapper.createArrayNode();
        ObjectNode bucket = mapper.createObjectNode();
        bucket.put(IResponseHandler.KEY, 1541980800000L);
        bucket.put(IResponseHandler.DOC_COUNT, 1);
        bucket.set("Count", mapper.createObjectNode().put(IResponseHandler.VALUE, 42));
        buckets.add(bucket);
        series.set(IResponseHandler.BUCKETS, buckets);
        aggs.set("Total Application", series);
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("line-test");
        request.setChartNode(chartNode);

        AggregateDto result = handler.translate(request, aggregations);

        assertThat(result.getChartType()).isEqualTo(ChartType.LINE);
        assertThat(result.getData()).hasSize(1);
        Data data = result.getData().get(0);
        assertThat(data.getPlots()).isNotEmpty();
        assertThat(data.getHeaderValue()).isEqualTo(42.0);
    }

    @Test
    void translate_requestIntervalOverridesChartConfig() throws Exception {
        chartNode.put("interval", "day");
        RequestDate requestDate = new RequestDate();
        requestDate.setInterval("month");
        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("line-test");
        request.setChartNode(chartNode);
        request.setRequestDate(requestDate);

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        ObjectNode series = mapper.createObjectNode();
        ArrayNode buckets = mapper.createArrayNode();
        ObjectNode bucket = mapper.createObjectNode();
        bucket.put(IResponseHandler.KEY, 1541980800000L);
        bucket.set("Count", mapper.createObjectNode().put(IResponseHandler.VALUE, 1));
        buckets.add(bucket);
        series.set(IResponseHandler.BUCKETS, buckets);
        aggs.set("Total Application", series);
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateDto result = handler.translate(request, aggregations);

        assertThat(result.getData().get(0).getPlots().get(0).getName()).contains("-");
    }

    @Test
    void translate_missingInterval_throws() {
        chartNode.put("interval", "");
        AggregateRequestDto request = new AggregateRequestDto();
        request.setChartNode(chartNode);
        ObjectNode aggregations = mapper.createObjectNode();
        aggregations.set(IResponseHandler.AGGREGATIONS, mapper.createObjectNode());

        assertThatThrownBy(() -> handler.translate(request, aggregations))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Interval");
    }
}
