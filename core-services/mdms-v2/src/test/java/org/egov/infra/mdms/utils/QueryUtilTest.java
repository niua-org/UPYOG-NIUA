package org.egov.infra.mdms.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QueryUtilTest {

    @Test
    void addClauseIfRequired_emptyPreparedList_appendsWhere() {
        StringBuilder query = new StringBuilder("SELECT 1");
        List<Object> params = new ArrayList<>();

        QueryUtil.addClauseIfRequired(query, params);

        assertThat(query.toString()).isEqualTo("SELECT 1 WHERE ");
    }

    @Test
    void addClauseIfRequired_nonEmptyPreparedList_appendsAnd() {
        StringBuilder query = new StringBuilder("SELECT 1 WHERE id = ?");
        List<Object> params = new ArrayList<>(List.of("x"));

        QueryUtil.addClauseIfRequired(query, params);

        assertThat(query.toString()).endsWith(" AND ");
    }

    @Test
    void createQuery_threeValues_returnsThreePlaceholders() {
        assertThat(QueryUtil.createQuery(3)).isEqualTo(" ?, ?, ?");
    }

    @Test
    void addToPreparedStatement_setOfIds_appendsAllToList() {
        List<Object> params = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>(List.of("a", "b"));

        QueryUtil.addToPreparedStatement(params, ids);

        assertThat(params).containsExactly("a", "b");
    }

    @Test
    void addOrderByClause_validClause_appendsOrderBy() {
        assertThat(QueryUtil.addOrderByClause("SELECT * FROM mdms", " ORDER BY id"))
                .isEqualTo("SELECT * FROM mdms ORDER BY id");
    }

    @Test
    void preparePartialJsonStringFromFilterMap_singleFilter_returnsJsonFragment() {
        String json = QueryUtil.preparePartialJsonStringFromFilterMap(Map.of("code", "A"));

        assertThat(json).contains("\"code\"");
        assertThat(json).contains("A");
    }
}
