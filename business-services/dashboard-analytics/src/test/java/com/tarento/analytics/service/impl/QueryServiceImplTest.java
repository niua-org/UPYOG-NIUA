package com.tarento.analytics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.dao.ElasticSearchDao;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.enums.ChartType;
import com.tarento.analytics.model.ElasticSearchDictator;
import com.tarento.analytics.model.KeyData;
import com.tarento.analytics.utils.ElasticProperties;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.sum.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.valuecount.ParsedValueCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceImplTest {

    @Mock
    private ElasticSearchDao elasticSearchDao;

    @InjectMocks
    private QueryServiceImpl queryService;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void getChartConfigurationQuery_mapsFiltersAndMergesAggregations() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setModuleLevel("PT");
        request.setFilters(new HashMap<>(Map.of("wardId", "W1", "tenantId", "all")));
        request.setEsFilters(new HashMap<>());

        ObjectNode query = mapper.createObjectNode();
        query.put(Constants.JsonPaths.AGGREGATION_QUERY, "{\"aggs\":{\"Total\":{\"sum\":{\"field\":\"amount\"}}}}");
        query.put(Constants.JsonPaths.REQUEST_QUERY_MAP,
                "{\"wardId\":\"domainObject.ward.keyword\",\"tenantId\":\"dataObject.tenantId\"}");
        query.put(Constants.JsonPaths.DATE_REF_FIELD, "dataObject.date");
        query.put(Constants.JsonPaths.MODULE, Constants.Modules.PT);

        ElasticSearchDictator dictator = new ElasticSearchDictator();
        dictator.setIndexName("idx");
        when(elasticSearchDao.createSearchDictatorV2(eq(request), eq("idx"), eq(""), eq("dataObject.date")))
                .thenReturn(dictator);

        SearchSourceBuilder source = new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        SearchRequest searchRequest = new SearchRequest("idx").source(source);
        when(elasticSearchDao.buildElasticSearchQuery(dictator)).thenReturn(searchRequest);

        ObjectNode result = queryService.getChartConfigurationQuery(request, query, "idx", "month");

        assertThat(result.has(Constants.JsonPaths.AGGS)).isTrue();
        assertThat(result.get(Constants.JsonPaths.AGGS).has("Total")).isTrue();
        assertThat(request.getEsFilters()).containsEntry("domainObject.ward.keyword", "W1");
        verify(elasticSearchDao).buildElasticSearchQuery(dictator);
    }

    @Test
    void getChartConfigurationQuery_commonModule_injectsModuleFilter() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setModuleLevel("TL");
        request.setFilters(new HashMap<>());
        request.setEsFilters(new HashMap<>());

        ObjectNode query = mapper.createObjectNode();
        query.put(Constants.JsonPaths.AGGREGATION_QUERY, "{\"aggs\":{}}");
        query.put(Constants.JsonPaths.REQUEST_QUERY_MAP, "{}");
        query.put(Constants.JsonPaths.DATE_REF_FIELD, "date");
        query.put(Constants.JsonPaths.MODULE, Constants.Modules.COMMON);

        ElasticSearchDictator dictator = new ElasticSearchDictator();
        when(elasticSearchDao.createSearchDictatorV2(any(), any(), any(), any())).thenReturn(dictator);
        when(elasticSearchDao.buildElasticSearchQuery(dictator))
                .thenReturn(new SearchRequest("idx").source(new SearchSourceBuilder()));

        queryService.getChartConfigurationQuery(request, query, "idx", null);

        assertThat(request.getFilters()).containsEntry(Constants.Filters.MODULE, "TL");
    }

    @Test
    void getChartConfigurationQuery_wrapsParsingFailures() throws Exception {
        AggregateRequestDto request = new AggregateRequestDto();
        request.setFilters(new HashMap<>());
        ObjectNode query = mapper.createObjectNode();
        query.put(Constants.JsonPaths.AGGREGATION_QUERY, "{\"aggs\":{}}");
        query.put(Constants.JsonPaths.REQUEST_QUERY_MAP, "not-json");
        query.put(Constants.JsonPaths.DATE_REF_FIELD, "date");
        query.put(Constants.JsonPaths.MODULE, Constants.Modules.PT);

        assertThatThrownBy(() -> queryService.getChartConfigurationQuery(request, query, "idx", null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAggregateLabelRecursively_collectsNestedLabels() {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put(ElasticProperties.Query.AGGREGATION_CONDITION.toLowerCase(), new LinkedHashMap<>());
        Map<String, Object> termProps = new LinkedHashMap<>();
        termProps.put(ElasticProperties.Query.LABEL.toLowerCase(), "Ward Label");
        root.put(ElasticProperties.Query.AGGREGATION_CONDITION.toLowerCase(), nested);
        root.put("wardAgg", termProps);

        Map<String, String> labels = new HashMap<>();
        queryService.getAggregateLabelRecursively(root, labels);

        assertThat(labels).containsEntry("wardAgg", "Ward Label");
    }

    @Test
    void parseParseLongTerms_extractsSumAvgAndCount() throws Exception {
        Terms.Bucket bucket = mock(Terms.Bucket.class);
        when(bucket.getKey()).thenReturn(99L);

        ParsedSum sum = mock(ParsedSum.class);
        when(sum.getValue()).thenReturn(10.5);
        ParsedAvg avg = mock(ParsedAvg.class);
        when(avg.getValue()).thenReturn(2.5);
        ParsedValueCount count = mock(ParsedValueCount.class);
        when(count.getValue()).thenReturn(3L);

        Map<String, org.elasticsearch.search.aggregations.Aggregation> aggMap = new LinkedHashMap<>();
        aggMap.put("field_sum", sum);
        aggMap.put("field_avg", avg);
        aggMap.put("field_count", count);
        org.elasticsearch.search.aggregations.Aggregations bucketAggs = mock(org.elasticsearch.search.aggregations.Aggregations.class);
        when(bucket.getAggregations()).thenReturn(bucketAggs);
        when(bucketAggs.getAsMap()).thenReturn(aggMap);

        ParsedLongTerms terms = mock(ParsedLongTerms.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        java.util.List bucketList = java.util.Collections.singletonList(bucket);
        when(terms.getBuckets()).thenReturn(bucketList);

        Map<String, String> labelMap = Map.of(
                ElasticProperties.Query.TERM.toLowerCase(), "Term",
                ElasticProperties.Query.SUM.toLowerCase(), "SumLabel");

        Method parse = QueryServiceImpl.class.getDeclaredMethod(
                "parseParseLongTerms", ParsedLongTerms.class, ChartType.class, Map.class);
        parse.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<KeyData, Object> result = (Map<KeyData, Object>) parse.invoke(
                queryService, terms, ChartType.LINE, labelMap);

        assertThat(result).hasSize(1);
        KeyData key = result.keySet().iterator().next();
        assertThat(key.getKey()).isEqualTo("99");
        Object value = result.get(key);
        assertThat(value).isInstanceOf(KeyData.class);
    }

    @Test
    void getFormattedDouble_roundsAndHandlesInfinite() throws Exception {
        Method format = QueryServiceImpl.class.getDeclaredMethod("getFormattedDouble", double.class);
        format.setAccessible(true);
        assertThat(format.invoke(queryService, 1.23456)).isEqualTo(1.23);
        assertThat(format.invoke(queryService, Double.POSITIVE_INFINITY)).isEqualTo(0.0);
    }
}
