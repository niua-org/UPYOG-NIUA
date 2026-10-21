package com.tarento.analytics.org.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.handler.IResponseHandler;
import com.tarento.analytics.handler.InsightsHandlerFactory;
import com.tarento.analytics.handler.ResponseHandlerFactory;
import com.tarento.analytics.service.QueryService;
import com.tarento.analytics.service.impl.RestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    @InjectMocks
    private TarentoServiceImpl tarentoService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void loadFixtureChartConfig() throws Exception {
        String json = Files.readString(
                Path.of("src/test/resources/fixtures/minimal-chart-api-config.json"));
        ObjectNode chartApiConfig = (ObjectNode) objectMapper.readTree(json);
        when(configurationLoader.get(Constants.ConfigurationFiles.CHART_API_CONFIG)).thenReturn(chartApiConfig);
        when(responseHandlerFactory.getInstance(any())).thenReturn(responseHandler);
        when(responseHandler.translate(any(), any())).thenReturn(new AggregateDto());
        ObjectNode queryNode = objectMapper.createObjectNode().put("query", "{}");
        when(queryService.getChartConfigurationQuery(any(), any(), anyString(), anyString())).thenReturn(queryNode);
        ObjectNode searchResponse = objectMapper.createObjectNode();
        searchResponse.set(Constants.JsonPaths.AGGREGATIONS, objectMapper.createObjectNode());
        when(restService.search(anyString(), anyString())).thenReturn(searchResponse);
    }

    @Test
    void getAggregatedData_mapsUlbFilterToTenantId() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("smokeTestChart");
        request.setModuleLevel(Constants.Modules.COMMON);
        Map<String, Object> filters = new HashMap<>();
        filters.put("ulb", "pg.citya");
        request.setFilters(filters);

        AggregateDto result = tarentoService.getAggregatedData(request, Collections.emptyList());

        assertThat(result).isNotNull();
        assertThat(request.getFilters().get("tenantId")).isEqualTo("pg.citya");
    }

    @Test
    void getAggregatedData_emptyFilters_doesNotThrow() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setVisualizationCode("smokeTestChart");
        request.setModuleLevel(Constants.Modules.COMMON);
        request.setFilters(new HashMap<>());

        AggregateDto result = tarentoService.getAggregatedData(request, Collections.emptyList());

        assertThat(result).isNotNull();
    }
}
