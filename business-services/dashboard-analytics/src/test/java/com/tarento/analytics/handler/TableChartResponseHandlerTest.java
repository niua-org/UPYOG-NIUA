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
import org.springframework.test.util.ReflectionTestUtils;

import static com.tarento.analytics.constant.Constants.STRING_DATATYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TableChartResponseHandlerTest {

    @Mock
    private ComputedFieldHelper computedFieldHelper;

    @InjectMocks
    private TableChartResponseHandler handler;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        ReflectionTestUtils.setField(handler, "targetacheivementChartListString", "targetViz");
    }

    @Test
    void translate_buildsRowsWithNumericAndStringColumns() throws Exception {
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(IResponseHandler.PLOT_LABEL, "Ward");
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.TABLE.toString());
        chartNode.put(IResponseHandler.POST_AGGREGATION_THEORY, "");
        ArrayNode paths = mapper.createArrayNode().add("Amount Collected").add("Ward Name");
        chartNode.set(IResponseHandler.AGGS_PATH, paths);
        ArrayNode typeMapping = mapper.createArrayNode();
        typeMapping.add(mapper.createObjectNode().put("Amount Collected", "amount"));
        typeMapping.add(mapper.createObjectNode().put("Ward Name", STRING_DATATYPE));
        chartNode.set(IResponseHandler.TYPE_MAPPING, typeMapping);

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        ArrayNode buckets = mapper.createArrayNode();
        ObjectNode bucket = mapper.createObjectNode();
        bucket.put(IResponseHandler.KEY, "Ward-1");
        bucket.set("Amount Collected", mapper.createObjectNode().put(IResponseHandler.VALUE, 150.0));
        ObjectNode wardName = mapper.createObjectNode();
        wardName.put(IResponseHandler.KEY, "Ward Alpha");
        bucket.set("Ward Name", wardName);
        buckets.add(bucket);
        aggs.set(IResponseHandler.BUCKETS, buckets);
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("tableViz");
        request.setChartNode(chartNode);

        AggregateDto result = handler.translate(request, aggregations);

        assertThat(result.getChartType()).isEqualTo(ChartType.TABLE);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getPlots()).hasSizeGreaterThan(2);
    }

    @Test
    void translate_targetAchievementChart_invokesComputedFieldHelper() throws Exception {
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(IResponseHandler.PLOT_LABEL, "ULB");
        chartNode.put(IResponseHandler.DRILL_CHART, "none");
        chartNode.put(IResponseHandler.CHART_TYPE, ChartType.TABLE.toString());
        chartNode.put(IResponseHandler.POST_AGGREGATION_THEORY, "targetTheory");
        ArrayNode paths = mapper.createArrayNode().add("Total Collection");
        chartNode.set(IResponseHandler.AGGS_PATH, paths);
        chartNode.set(IResponseHandler.TYPE_MAPPING,
                mapper.createArrayNode().add(mapper.createObjectNode().put("Total Collection", "amount")));

        ObjectNode aggregations = mapper.createObjectNode();
        ObjectNode aggs = mapper.createObjectNode();
        ArrayNode buckets = mapper.createArrayNode();
        ObjectNode bucket = mapper.createObjectNode();
        bucket.put(IResponseHandler.KEY, "City-A");
        bucket.set("Total Collection", mapper.createObjectNode().put(IResponseHandler.VALUE, 200.0));
        buckets.add(bucket);
        aggs.set(IResponseHandler.BUCKETS, buckets);
        aggregations.set(IResponseHandler.AGGREGATIONS, aggs);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("targetViz");
        request.setChartNode(chartNode);

        handler.translate(request, aggregations);

        verify(computedFieldHelper).set(request, "targetTheory");
        verify(computedFieldHelper).add(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(IResponseHandler.TARGET_ACHIEVED),
                org.mockito.ArgumentMatchers.eq(IResponseHandler.TOTAL_COLLECTION),
                org.mockito.ArgumentMatchers.eq(IResponseHandler.TARGET_COLLECTION));
    }
}
