package com.tarento.analytics.dao.impl;

import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.model.ElasticSearchDictator;
import com.tarento.analytics.utils.ElasticProperties;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticSearchDaoImplAdditionalQueryTest {

    private static ElasticSearchDaoImpl dao;

    @BeforeAll
    static void initDao() throws MalformedURLException {
        dao = new ElasticSearchDaoImpl("localhost", 9200);
    }

    @Test
    void createSearchDictatorV2_blankEsFilters_skipsMatchMap() throws Exception {
        AggregateRequestDto dto = new AggregateRequestDto();
        dto.setEsFilters(new HashMap<>());

        ElasticSearchDictator dictator = dao.createSearchDictatorV2(dto, "idx", "_doc", "date");

        assertThat(dictator.getIndexName()).isEqualTo("idx");
        assertThat(dictator.getQueryMap()).doesNotContainKey(ElasticProperties.Query.MATCH_CONDITION);
    }

    @Test
    void createSearchDictatorV2_scalarEsFilter_addsMatchCondition() throws Exception {
        AggregateRequestDto dto = new AggregateRequestDto();
        dto.setEsFilters(new HashMap<>(Map.of("status.keyword", "ACTIVE")));

        ElasticSearchDictator dictator = dao.createSearchDictatorV2(dto, "idx", "_doc", "date");

        assertThat(dictator.getQueryMap().get(ElasticProperties.Query.MATCH_CONDITION).get("status.keyword"))
                .containsExactly("ACTIVE");
    }
}
