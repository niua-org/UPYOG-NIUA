package org.egov.tlcalculator.repository.builder;

import org.egov.tlcalculator.web.models.CalculationSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationQueryBuilderTest {

    private final CalculationQueryBuilder queryBuilder = new CalculationQueryBuilder();

    @Test
    void getSearchQuery_bindsTenantAndApplicationNumber() {
        List<Object> params = new ArrayList<>();
        CalculationSearchCriteria criteria = new CalculationSearchCriteria();
        criteria.setTenantId("pb.amritsar");
        criteria.setAplicationNumber("TL-001");

        String sql = queryBuilder.getSearchQuery(criteria, params);

        assertThat(sql).contains("tp.tenantid=?");
        assertThat(sql).contains("tp.consumercode=?");
        assertThat(sql).contains("LIMIT 1");
        assertThat(params).containsExactly("pb.amritsar", "TL-001");
    }
}
