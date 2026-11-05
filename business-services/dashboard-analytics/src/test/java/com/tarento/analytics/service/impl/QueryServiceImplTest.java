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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceImplTest {

    @Mock
    private ElasticSearchDao elasticSearchDao;

    @InjectMocks
    private QueryServiceImpl queryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getChartConfigurationQuery_mergesAggregationIntoSearchBody() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setModuleLevel("PT");
        request.setFilters(new HashMap<>());

        ObjectNode query = objectMapper.createObjectNode();
        query.put(Constants.JsonPaths.AGGREGATION_QUERY, "{\"aggs\":{\"total\":{\"sum\":{\"field\":\"amount\"}}}}");
        query.put(Constants.JsonPaths.REQUEST_QUERY_MAP, "{}");
        query.put(Constants.JsonPaths.DATE_REF_FIELD, "@timestamp");
        query.put(Constants.JsonPaths.MODULE, Constants.Modules.COMMON);

        ElasticSearchDictator dictator = new ElasticSearchDictator();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        SearchRequest searchRequest = new SearchRequest("billing-index").source(sourceBuilder);

        when(elasticSearchDao.createSearchDictatorV2(any(), anyString(), anyString(), anyString()))
                .thenReturn(dictator);
        when(elasticSearchDao.buildElasticSearchQuery(dictator)).thenReturn(searchRequest);

        ObjectNode result = queryService.getChartConfigurationQuery(request, query, "billing-index", "month");

        assertThat(result).isNotNull();
        assertThat(result.get(Constants.JsonPaths.AGGS)).isNotNull();
        assertThat(result.get(Constants.JsonPaths.AGGS).get("total")).isNotNull();
    }
}
