package org.egov.swcalculation.repository.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SWCalculatorQueryBuilderTest {

    @InjectMocks
    private SWCalculatorQueryBuilder queryBuilder;

    @Test
    void getDistinctTenantIds_returnsSewerageTenantQuery() {
        assertThat(queryBuilder.getDistinctTenantIds())
                .contains("eg_sw_connection")
                .contains("distinct(tenantid)");
    }

    @Test
    void getCountQuery_returnsConnectionCountSql() {
        assertThat(queryBuilder.getCountQuery()).contains("eg_sw_connection");
    }
}
