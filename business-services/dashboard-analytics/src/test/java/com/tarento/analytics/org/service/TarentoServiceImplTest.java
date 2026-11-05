package com.tarento.analytics.org.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.RequestDate;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.handler.IResponseHandler;
import com.tarento.analytics.handler.InsightsHandler;
import com.tarento.analytics.handler.InsightsHandlerFactory;
import com.tarento.analytics.handler.ResponseHandlerFactory;
import com.tarento.analytics.model.InsightsConfiguration;
import com.tarento.analytics.service.QueryService;
import com.tarento.analytics.service.impl.RestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TarentoServiceImplTest {

    @Mock
    private QueryService queryService;
    @Mock
    private RestService restService;
    @Mock
    private ConfigurationLoader configurationLoader;
    @Mock
    private ResponseHandlerFactory responseHandlerFactory;
    @Mock
    private InsightsHandlerFactory insightsHandlerFactory;
    @Mock
    private MdmsApiMappings mdmsApiMappings;
    @Mock
    private IResponseHandler responseHandler;
    @Mock
    private InsightsHandler insightsHandler;

    @InjectMocks
    private TarentoServiceImpl tarentoService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ObjectNode chartApiConfig;

    @BeforeEach
    void loadFixtureChartConfig() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/fixtures/minimal-chart-api-config.json"));
        chartApiConfig = (ObjectNode) objectMapper.readTree(json);
        when(configurationLoader.get(Constants.ConfigurationFiles.CHART_API_CONFIG)).thenReturn(chartApiConfig);
        when(responseHandlerFactory.getInstance(any())).thenReturn(responseHandler);
        when(responseHandler.translate(any(), any())).thenReturn(new AggregateDto());
        ObjectNode queryNode = objectMapper.createObjectNode().put("query", "{}");
        when(queryService.getChartConfigurationQuery(any(), any(), anyString(), anyString())).thenReturn(queryNode);
        ObjectNode searchResponse = objectMapper.createObjectNode();
        searchResponse.set(Constants.JsonPaths.AGGREGATIONS, objectMapper.createObjectNode().put("count", 1));
        when(restService.search(anyString(), anyString())).thenReturn(searchResponse);
    }

    private AggregateRequestDto baseRequest(String visualizationCode, String moduleLevel) {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode(visualizationCode);
        request.setModuleLevel(moduleLevel);
        request.setFilters(new HashMap<>());
        return request;
    }

    @Test
    void getAggregatedData_mapsUlbFilterToTenantId() throws Exception {
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        request.getFilters().put("ulb", "pg.citya");

        tarentoService.getAggregatedData(request, Collections.emptyList());

        assertThat(request.getFilters().get("tenantId")).isEqualTo("pg.citya");
    }

    @Test
    void getAggregatedData_emptyFilters_doesNotThrow() throws Exception {
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);

        AggregateDto result = tarentoService.getAggregatedData(request, Collections.emptyList());

        assertThat(result).isNotNull();
    }

    @Test
    void daysBetween_returnsAbsoluteDayDifference() {
        long start = 0L;
        long end = TimeUnit.DAYS.toMillis(10);

        assertThat(tarentoService.daysBetween(start, end)).isEqualTo(10);
        assertThat(tarentoService.daysBetween(end, start)).isEqualTo(10);
    }

    @Test
    void getAggregatedData_replacesIndexWildcardWithMonthWhenIntervalMissing() throws Exception {
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(restService).search(eq("test-index-month"), anyString());
    }

    @Test
    void getAggregatedData_usesRequestIntervalForIndexWildcard() throws Exception {
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        RequestDate requestDate = new RequestDate();
        requestDate.setInterval("week");
        request.setRequestDate(requestDate);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(restService).search(eq("test-index-week"), anyString());
    }

    @Test
    void getAggregatedData_mapsYearIntervalToMonthInIndexName() throws Exception {
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        RequestDate requestDate = new RequestDate();
        requestDate.setInterval("year");
        request.setRequestDate(requestDate);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(restService).search(eq("test-index-month"), anyString());
    }

    @Test
    void getAggregatedData_skipsElasticsearchForNonMatchingModule() throws Exception {
        ObjectNode wsOnlyChart = objectMapper.createObjectNode();
        wsOnlyChart.put(Constants.JsonPaths.CHART_TYPE, ChartType.METRIC.toString());
        ArrayNode queries = objectMapper.createArrayNode();
        queries.add(objectMapper.createObjectNode()
                .put(Constants.JsonPaths.MODULE, "WS")
                .put(Constants.JsonPaths.INDEX_NAME, "ws-index")
                .put(Constants.JsonPaths.AGGREGATION_QUERY, "{}")
                .put(Constants.JsonPaths.REQUEST_QUERY_MAP, "{}")
                .put(Constants.JsonPaths.DATE_REF_FIELD, "@timestamp"));
        wsOnlyChart.set(Constants.JsonPaths.QUERIES, queries);
        chartApiConfig.set("wsOnlyChart", wsOnlyChart);

        AggregateRequestDto request = baseRequest("wsOnlyChart", Constants.Modules.PT);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(restService, never()).search(anyString(), anyString());
        verify(responseHandler, never()).translate(any(), any());
    }

    @Test
    void getAggregatedData_runsCommonModuleQueryForAnyModuleLevel() throws Exception {
        ObjectNode ptChart = objectMapper.createObjectNode();
        ptChart.put(Constants.JsonPaths.CHART_TYPE, ChartType.METRIC.toString());
        ArrayNode queries = objectMapper.createArrayNode();
        queries.add(objectMapper.createObjectNode()
                .put(Constants.JsonPaths.MODULE, Constants.Modules.COMMON)
                .put(Constants.JsonPaths.INDEX_NAME, "common-index")
                .put(Constants.JsonPaths.AGGREGATION_QUERY, "{}")
                .put(Constants.JsonPaths.REQUEST_QUERY_MAP, "{}")
                .put(Constants.JsonPaths.DATE_REF_FIELD, "@timestamp"));
        ptChart.set(Constants.JsonPaths.QUERIES, queries);
        chartApiConfig.set("commonOnlyChart", ptChart);

        AggregateRequestDto request = baseRequest("commonOnlyChart", Constants.Modules.PT);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(restService).search(eq("common-index"), anyString());
    }

    @Test
    void getAggregatedData_invokesResponseHandlerWhenAggregationsPresent() throws Exception {
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        AggregateDto translated = new AggregateDto();
        when(responseHandler.translate(any(), any())).thenReturn(translated);

        AggregateDto result = tarentoService.getAggregatedData(request, Collections.emptyList());

        assertThat(result).isSameAs(translated);
        verify(responseHandlerFactory).getInstance(ChartType.METRIC);
        verify(responseHandler).translate(eq(request), any());
    }

    @Test
    void getAggregatedData_passesEmptyAggregationNodesWhenSearchReturnsNull() throws Exception {
        when(restService.search(anyString(), anyString())).thenReturn(null);
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(responseHandler).translate(eq(request), org.mockito.ArgumentMatchers.argThat(aggrNode ->
                aggrNode.has(Constants.JsonPaths.AGGREGATIONS)
                        && !aggrNode.get(Constants.JsonPaths.AGGREGATIONS).fields().hasNext()));
    }

    @Test
    void getAggregatedData_wrapsSearchFailureInRuntimeException() {
        when(restService.search(anyString(), anyString())).thenThrow(new RuntimeException("ES down"));
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);

        assertThatThrownBy(() -> tarentoService.getAggregatedData(request, Collections.emptyList()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ES down");
    }

    @Test
    void getAggregatedData_restoresOriginalIndexNameAfterProcessing() throws Exception {
        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        RequestDate requestDate = new RequestDate();
        requestDate.setInterval("week");
        request.setRequestDate(requestDate);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        ArrayNode queries = (ArrayNode) chartApiConfig.get("smokeTestChart").get(Constants.JsonPaths.QUERIES);
        assertThat(queries.get(0).get(Constants.JsonPaths.INDEX_NAME).asText()).isEqualTo("test-index-*");
    }

    @Test
    void getAggregatedData_translatesTenantIdWhenChartRequestsTranslation() throws Exception {
        ObjectNode chart = (ObjectNode) chartApiConfig.get("smokeTestChart");
        chart.put(Constants.JsonPaths.TRANSLATE_CODE, true);
        when(mdmsApiMappings.valueOf("pb.city1")).thenReturn("City One");

        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        request.getFilters().put(Constants.MDMSKeys.TENANT_ID, "pb.city1");

        tarentoService.getAggregatedData(request, Collections.emptyList());

        assertThat(request.getFilters().get(Constants.MDMSKeys.TENANT_ID)).isEqualTo("City One");
        verify(mdmsApiMappings).valueOf("pb.city1");
    }

    @Test
    void getAggregatedData_runsInsightQueriesWhenIntervalWithinWindow() throws Exception {
        ObjectNode chart = (ObjectNode) chartApiConfig.get("smokeTestChart");
        InsightsConfiguration insightConfig = new InsightsConfiguration();
        insightConfig.setInsightInterval(Constants.Interval.month.toString());
        chart.set(Constants.JsonPaths.INSIGHT, objectMapper.valueToTree(insightConfig));
        when(insightsHandlerFactory.getInstance(ChartType.METRIC)).thenReturn(insightsHandler);
        when(insightsHandler.getInsights(any(), anyString(), anyString(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        RequestDate requestDate = new RequestDate();
        long end = System.currentTimeMillis();
        long start = end - TimeUnit.DAYS.toMillis(7);
        requestDate.setStartDate(String.valueOf(start));
        requestDate.setEndDate(String.valueOf(end));
        requestDate.setInterval("month");
        request.setRequestDate(requestDate);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(restService, times(2)).search(eq("test-index-month"), anyString());
        verify(insightsHandlerFactory).getInstance(ChartType.METRIC);
        ArgumentCaptor<String> visualizationCaptor = ArgumentCaptor.forClass(String.class);
        verify(insightsHandler).getInsights(any(), visualizationCaptor.capture(), anyString(), any(), any());
        assertThat(visualizationCaptor.getValue()).isEqualTo("ismokeTestChart");
    }

    @Test
    void getAggregatedData_skipsInsightsWhenDateRangeExceedsMonthWindow() throws Exception {
        ObjectNode chart = (ObjectNode) chartApiConfig.get("smokeTestChart");
        InsightsConfiguration insightConfig = new InsightsConfiguration();
        insightConfig.setInsightInterval(Constants.Interval.month.toString());
        chart.set(Constants.JsonPaths.INSIGHT, objectMapper.valueToTree(insightConfig));

        AggregateRequestDto request = baseRequest("smokeTestChart", Constants.Modules.COMMON);
        RequestDate requestDate = new RequestDate();
        long end = System.currentTimeMillis();
        long start = end - TimeUnit.DAYS.toMillis(40);
        requestDate.setStartDate(String.valueOf(start));
        requestDate.setEndDate(String.valueOf(end));
        request.setRequestDate(requestDate);

        tarentoService.getAggregatedData(request, Collections.emptyList());

        verify(restService, times(1)).search(anyString(), anyString());
        verify(insightsHandlerFactory, never()).getInstance(any());
    }
}
