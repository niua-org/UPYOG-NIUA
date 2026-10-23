package com.tarento.analytics.org.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.handler.IResponseHandler;
import com.tarento.analytics.handler.ResponseHandlerFactory;
import com.tarento.analytics.service.QueryService;
import com.tarento.analytics.service.impl.RestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdmsServiceImplTest {

    @Mock
    private QueryService queryService;
    @Mock
    private RestService restService;
    @Mock
    private ConfigurationLoader configurationLoader;
    @Mock
    private ResponseHandlerFactory responseHandlerFactory;
    @Mock
    private MdmsApiMappings mdmsApiMappings;
    @Mock
    private IResponseHandler responseHandler;

    @InjectMocks
    private MdmsServiceImpl mdmsServiceImpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mdmsServiceImpl, "groupTenantIds", Map.of());
    }

    @Test
    void getAggregatedData_buildsResponseViaHandler() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode chartNode = mapper.createObjectNode();
        chartNode.put(Constants.JsonPaths.CHART_TYPE, ChartType.METRIC.toString());
        chartNode.put("plotLabel", "DDR");
        ArrayNode queries = mapper.createArrayNode();
        ObjectNode query = mapper.createObjectNode();
        query.put(Constants.JsonPaths.MODULE, "PT");
        query.put(Constants.JsonPaths.INDEX_NAME, "billing");
        query.put(Constants.JsonPaths.AGGREGATION_QUERY, "{\"aggs\":{\"m\":{\"value_count\":{\"field\":\"x\"}}}}");
        query.put(Constants.JsonPaths.REQUEST_QUERY_MAP, "{}");
        query.put(Constants.JsonPaths.DATE_REF_FIELD, "@timestamp");
        queries.add(query);
        chartNode.set(Constants.JsonPaths.QUERIES, queries);
        root.set("viz-1", chartNode);

        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("viz-1");
        request.setModuleLevel("PT");
        request.setFilters(new HashMap<>(Map.of("tenantId", List.of("pb.city1"))));

        when(configurationLoader.get(Constants.ConfigurationFiles.CHART_API_CONFIG)).thenReturn(root);
        when(mdmsApiMappings.getGroupedTenants(any())).thenReturn(Map.of());
        when(mdmsApiMappings.getAll()).thenReturn(Map.of("DDR-A", List.of("pb.city1")));
        when(queryService.getChartConfigurationQuery(any(), any(), any(), any()))
                .thenReturn(mapper.createObjectNode().put("query", "ok"));
        when(restService.search(any(), any())).thenReturn(mapper.createObjectNode()
                .set(Constants.JsonPaths.AGGREGATIONS, mapper.createObjectNode().put("key", "DDR-A")));
        when(responseHandlerFactory.getInstance(ChartType.METRIC)).thenReturn(responseHandler);
        when(responseHandler.translate(any(), any())).thenReturn(new AggregateDto());

        AggregateDto result = mdmsServiceImpl.getAggregatedData(request, List.of());

        assertThat(result).isNotNull();
    }
}
