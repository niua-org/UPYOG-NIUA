package com.tarento.analytics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dao.ElasticSearchDao;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.model.ElasticSearchDictator;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceImplBehaviorTest {

    @Mock
    private ElasticSearchDao elasticSearchDao;

    @InjectMocks
    private QueryServiceImpl queryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getChartConfigurationQuery_mapsTenantFilterToEsFilters() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setModuleLevel("PT");
        Map<String, Object> filters = new HashMap<>();
        filters.put("tenantId", "pb.city1");
        request.setFilters(filters);

        ObjectNode query = objectMapper.createObjectNode();
        query.put(Constants.JsonPaths.AGGREGATION_QUERY, "{\"aggs\":{\"total\":{\"sum\":{\"field\":\"amount\"}}}}");
        query.put(Constants.JsonPaths.REQUEST_QUERY_MAP, "{\"tenantId\":\"tenantId.keyword\"}");
        query.put(Constants.JsonPaths.DATE_REF_FIELD, "@timestamp");
        query.put(Constants.JsonPaths.MODULE, Constants.Modules.COMMON);

        ElasticSearchDictator dictator = new ElasticSearchDictator();
        SearchRequest searchRequest = new SearchRequest("billing-index").source(
                new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));
        when(elasticSearchDao.createSearchDictatorV2(any(), anyString(), anyString(), anyString())).thenReturn(dictator);
        when(elasticSearchDao.buildElasticSearchQuery(dictator)).thenReturn(searchRequest);

        ObjectNode result = queryService.getChartConfigurationQuery(request, query, "billing-index", "month");

        assertThat(request.getEsFilters()).containsEntry("tenantId.keyword", "pb.city1");
        assertThat(result.get(Constants.JsonPaths.AGGS)).isNotNull();
    }

    @Test
    void getChartConfigurationQuery_skipsFilterAllValues() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setModuleLevel(Constants.Modules.HOME_REVENUE);
        request.setFilters(new HashMap<>(Map.of("tenantId", Constants.Filters.FILTER_ALL)));

        ObjectNode query = objectMapper.createObjectNode();
        query.put(Constants.JsonPaths.AGGREGATION_QUERY, "{\"aggs\":{\"c\":{\"value_count\":{\"field\":\"id\"}}}}");
        query.put(Constants.JsonPaths.REQUEST_QUERY_MAP, "{\"tenantId\":\"tenantId.keyword\"}");
        query.put(Constants.JsonPaths.DATE_REF_FIELD, "@timestamp");
        query.put(Constants.JsonPaths.MODULE, Constants.Modules.COMMON);

        ElasticSearchDictator dictator = new ElasticSearchDictator();
        SearchRequest searchRequest = new SearchRequest("idx").source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));
        when(elasticSearchDao.createSearchDictatorV2(any(), anyString(), anyString(), anyString())).thenReturn(dictator);
        when(elasticSearchDao.buildElasticSearchQuery(dictator)).thenReturn(searchRequest);

        ObjectNode result = queryService.getChartConfigurationQuery(request, query, "idx", null);

        assertThat(result).isNotNull();
        assertThat(request.getEsFilters()).isEmpty();
    }
}
