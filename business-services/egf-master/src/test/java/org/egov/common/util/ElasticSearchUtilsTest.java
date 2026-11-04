package org.egov.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticSearchUtilsTest {

    private ElasticSearchUtils elasticSearchUtils;
    private BoolQueryBuilder boolQueryBuilder;

    @BeforeEach
    void setUp() {
        elasticSearchUtils = new ElasticSearchUtils();
        boolQueryBuilder = QueryBuilders.boolQuery();
    }

    @Test
    void add_filtersWhenValuePresent() {
        elasticSearchUtils.add("value", "field", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isNotEmpty();
    }

    @Test
    void add_skipsWhenValueNull() {
        elasticSearchUtils.add(null, "field", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isEmpty();
    }

    @Test
    void in_filtersWhenListPresent() {
        elasticSearchUtils.in(Arrays.asList("a", "b"), "field", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isNotEmpty();
    }

    @Test
    void in_skipsWhenListNull() {
        elasticSearchUtils.in(null, "field", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isEmpty();
    }

    @Test
    void gte_addsRangeFromWhenValuePresent() {
        elasticSearchUtils.gte(10, "amount", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isNotEmpty();
    }

    @Test
    void lte_addsRangeToWhenValuePresent() {
        elasticSearchUtils.lte(99, "amount", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isNotEmpty();
    }

    @Test
    void gte_skipsWhenValueNull() {
        elasticSearchUtils.gte(null, "amount", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isEmpty();
    }

    @Test
    void in_acceptsEmptyList() {
        elasticSearchUtils.in(Collections.emptyList(), "field", boolQueryBuilder);
        assertThat(boolQueryBuilder.filter()).isNotEmpty();
    }
}
