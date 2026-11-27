package org.egov.wscalculation.repository.builder;

import org.egov.wscalculation.config.WSCalculationConfiguration;
import org.egov.wscalculation.web.models.MeterReadingSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WSCalculatorQueryBuilderTest {

    @Mock
    private WSCalculationConfiguration config;

    @InjectMocks
    private WSCalculatorQueryBuilder queryBuilder;

    @Test
    void getDistinctTenantIds_returnsStaticCriteria() {
        assertThat(queryBuilder.getDistinctTenantIds())
                .contains("distinct(tenantid)")
                .contains("eg_ws_connection");
    }

    @Test
    void getSearchQueryString_whenEmptyCriteria_returnsNull() {
        assertThat(queryBuilder.getSearchQueryString(MeterReadingSearchCriteria.builder().build(), new ArrayList<>()))
                .isNull();
    }

    @Test
    void getSearchQueryString_withTenantAndConnections_buildsWhereClause() {
        when(config.getMeterReadingDefaultLimit()).thenReturn(100);
        when(config.getMeterReadingDefaultOffset()).thenReturn(0);

        Set<String> connectionNos = new LinkedHashSet<>(List.of("WS-101", "WS-102"));
        MeterReadingSearchCriteria criteria = MeterReadingSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .connectionNos(connectionNos)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSearchQueryString(criteria, params);

        assertThat(sql).contains("mr.tenantid= ?");
        assertThat(sql).contains("mr.connectionNo IN");
        assertThat(sql).contains("OFFSET ? LIMIT ?");
        assertThat(params).containsExactly("pb.amritsar", "WS-101", "WS-102", 0, 100);
    }

    @Test
    void getNoOfMeterReadingConnectionQuery_expandsConnectionIdPlaceholders() {
        List<Object> params = new ArrayList<>();
        Set<String> ids = Set.of("WS-1");

        String sql = queryBuilder.getNoOfMeterReadingConnectionQuery(ids, params);

        assertThat(sql).contains("connectionNo in");
        assertThat(params).containsExactly("WS-1");
    }
}
