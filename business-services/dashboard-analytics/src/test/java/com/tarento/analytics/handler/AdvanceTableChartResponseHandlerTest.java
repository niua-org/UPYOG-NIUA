package com.tarento.analytics.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.helper.ComputedFieldHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tarento.analytics.constant.Constants.STRING_DATATYPE;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdvanceTableChartResponseHandlerTest {

    @Mock
    private ComputedFieldHelper computedFieldHelper;

    @InjectMocks
    private AdvanceTableChartResponseHandler handler;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void translate_buildsAdvanceTableRows() throws Exception {
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(IResponseHandler.PLOT_LABEL, "District");
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.XTABLE.toString());
        chartNode.put(IResponseHandler.POST_AGGREGATION_THEORY, "");
        chartNode.set(IResponseHandler.AGGS_PATH, mapper.createArrayNode().add("Applications"));
        ArrayNode typeMapping = mapper.createArrayNode();
        typeMapping.add(mapper.createObjectNode().put("Applications", STRING_DATATYPE));
        chartNode.set(IResponseHandler.TYPE_MAPPING, typeMapping);

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        ArrayNode buckets = mapper.createArrayNode();
        ObjectNode bucket = mapper.createObjectNode();
        bucket.put(IResponseHandler.KEY, "District-1");
        bucket.set("Applications", mapper.createObjectNode().put(IResponseHandler.VALUE, 10));
        buckets.add(bucket);
        aggs.set(IResponseHandler.BUCKETS, buckets);
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("xtable");
        request.setChartNode(chartNode);

        AggregateDto result = handler.translate(request, aggregations);

        assertThat(result.getData()).isNotEmpty();
    }
}
