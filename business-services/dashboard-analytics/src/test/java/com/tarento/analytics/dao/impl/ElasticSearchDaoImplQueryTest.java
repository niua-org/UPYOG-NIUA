package com.tarento.analytics.dao.impl;

import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.RequestDate;
import com.tarento.analytics.model.ElasticSearchDictator;
import com.tarento.analytics.utils.ElasticProperties;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchDaoImplQueryTest {

    private static ElasticSearchDaoImpl dao;

    @BeforeAll
    static void initDao() throws MalformedURLException {
        dao = new ElasticSearchDaoImpl("localhost", 9200);
    }

    @Test
    void buildElasticSearchQuery_withoutAggregations_buildsBoolQueryOnly() {
        ElasticSearchDictator dictator = baseDictator();
        dictator.setQueryAggregationMap(null);

        SearchRequest request = dao.buildElasticSearchQuery(dictator);

        assertThat(request.indices()[0]).isEqualTo("test-index");
        assertThat(request.source().toString()).contains("bool");
    }

    @Test
    void buildElasticSearchQuery_withRangeAndMatch_appliesFilters() {
        ElasticSearchDictator dictator = baseDictator();
        Map<String, Map<String, List<Object>>> queryMap = new HashMap<>();
        Map<String, List<Object>> range = Map.of("dataObject.date", List.of("1585679400000", "1617215399000"));
        queryMap.put(ElasticProperties.Query.RANGE_CONDITION, range);
        Map<String, List<Object>> match = new HashMap<>();
        match.put("dataObject.tenantId.keyword", List.of("pb.amritsar"));
        queryMap.put(ElasticProperties.Query.MATCH_CONDITION, match);
        dictator.setQueryMap(queryMap);
        dictator.setQueryAggregationMap(null);

        SearchRequest request = dao.buildElasticSearchQuery(dictator);
        String source = request.source().toString();

        assertThat(source).contains("range");
        assertThat(source).contains("dataObject.date");
        assertThat(source).contains("terms");
    }

    @Test
    void buildElasticSearchQuery_withSumAndTermsAggregations() {
        ElasticSearchDictator dictator = baseDictator();
        Map<String, Object> aggMap = new LinkedHashMap<>();

        Map<String, Object> sumInner = new LinkedHashMap<>();
        sumInner.put("field", "dataObject.amount");
        sumInner.put("label", "Amount Collected");
        aggMap.put("SUM", sumInner);

        Map<String, Object> termInner = new LinkedHashMap<>();
        termInner.put("field", "domainObject.ward.keyword");
        termInner.put("label", "Ward");
        termInner.put("size", 10);
        aggMap.put("TERMS", termInner);

        dictator.setQueryAggregationMap(aggMap);

        SearchRequest request = dao.buildElasticSearchQuery(dictator);
        String source = request.source().toString();

        assertThat(source).contains("sum");
        assertThat(source).contains("Amount Collected");
        assertThat(source).contains("terms");
        assertThat(source).contains("Ward");
    }

    @Test
    void createSearchDictatorV2_mapsEsFiltersAndDateRange() throws Exception {
        AggregateRequestDto dto = new AggregateRequestDto();
        dto.setEsFilters(new HashMap<>(Map.of("dataObject.tenantId", "uat: pb.test")));
        RequestDate requestDate = new RequestDate();
        requestDate.setStartDate("1585679400000");
        requestDate.setEndDate("1617215399000");
        dto.setRequestDate(requestDate);

        ElasticSearchDictator dictator = dao.createSearchDictatorV2(dto, "idx", "_doc", "date");

        assertThat(dictator.getIndexName()).isEqualTo("idx");
        assertThat(dictator.getQueryMap()).containsKey(ElasticProperties.Query.MATCH_CONDITION);
        Map<String, List<Object>> match = dictator.getQueryMap().get(ElasticProperties.Query.MATCH_CONDITION);
        assertThat(match.get("dataObject.tenantId")).containsExactly("pb.test");
        assertThat(dictator.getQueryMap()).containsKey(ElasticProperties.Query.RANGE_CONDITION);
    }

    @Test
    void createSearchDictatorV2_financialYear_usesMatchInsteadOfRange() throws Exception {
        AggregateRequestDto dto = new AggregateRequestDto();
        RequestDate requestDate = new RequestDate();
        requestDate.setStartDate("1585679400000");
        requestDate.setEndDate("1704067199000");
        dto.setRequestDate(requestDate);

        ElasticSearchDictator dictator = dao.createSearchDictatorV2(dto, "idx", "_doc", "financialYear.keyword");

        Map<String, List<Object>> match = dictator.getQueryMap().get(ElasticProperties.Query.MATCH_CONDITION);
        assertThat(match.get("financialYear.keyword")).isNotEmpty();
    }

    @Test
    void createSearchDictatorV2_arrayTenantFilter_stripsUatPrefix() throws Exception {
        AggregateRequestDto dto = new AggregateRequestDto();
        List<Object> tenants = new ArrayList<>();
        tenants.add("uat: pb.a");
        tenants.add("pb.b");
        dto.setEsFilters(new HashMap<>(Map.of("dataObject.tenantId", tenants)));

        ElasticSearchDictator dictator = dao.createSearchDictatorV2(dto, "idx", "_doc", "date");

        Map<String, List<Object>> match = dictator.getQueryMap().get(ElasticProperties.Query.MATCH_CONDITION);
        assertThat(match.get("dataObject.tenantId")).containsExactly("pb.a", "pb.b");
    }

    private static ElasticSearchDictator baseDictator() {
        ElasticSearchDictator dictator = new ElasticSearchDictator();
        dictator.setIndexName("test-index");
        dictator.setDocumentType("_doc");
        dictator.setQueryMap(new HashMap<>());
        return dictator;
    }
}
