package com.tarento.analytics.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.enums.ChartType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PieChartResponseHandlerTest {

    private final PieChartResponseHandler handler = new PieChartResponseHandler();
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void translate_mergesBucketValuesIntoPlots() throws Exception {
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(IResponseHandler.CHART_NAME, "Status split");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.PIE.toString());
        chartNode.put(IResponseHandler.VALUE_TYPE, "number");
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.set(IResponseHandler.AGGS_PATH, mapper.createArrayNode().add("By Status"));

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        ObjectNode byStatus = mapper.createObjectNode();
        ArrayNode buckets = mapper.createArrayNode();
        ObjectNode paid = mapper.createObjectNode();
        paid.put(IResponseHandler.KEY, "PAID");
        paid.put(IResponseHandler.DOC_COUNT, 3);
        buckets.add(paid);
        ObjectNode pending = mapper.createObjectNode();
        pending.put(IResponseHandler.KEY, "PENDING");
        pending.put(IResponseHandler.DOC_COUNT, 2);
        buckets.add(pending);
        byStatus.set(IResponseHandler.BUCKETS, buckets);
        aggs.set("By Status", byStatus);
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setChartNode(chartNode);
        request.setVisualizationCode("pie-viz");

        AggregateDto dto = handler.translate(request, aggregations);

        assertThat(dto.getData()).hasSize(1);
        assertThat(dto.getData().get(0).getPlots()).hasSize(2);
    }
}
