package org.egov.fsm.calculator.repository.querybuilder;

import org.egov.fsm.calculator.config.BillingSlabConfig;
import org.egov.fsm.calculator.web.models.BillingSlabSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingSlabQueryBuilderTest {

    @Mock
    private BillingSlabConfig config;

    @InjectMocks
    private BillingSlabQueryBuilder queryBuilder;

    @Test
    void getBillingSlabExistQuery_addsIdParameter() {
        List<Object> params = new ArrayList<>();
        assertThat(queryBuilder.getBillingSlabExistQuery("slab-1", params)).contains("eg_billing_slab");
        assertThat(params).containsExactly("slab-1");
    }

    @Test
    void getBillingSlabCombinationCountQuery_collectsAllBindParameters() {
        List<Object> params = new ArrayList<>();
        String sql = queryBuilder.getBillingSlabCombinationCountQuery(
                "pb", BigDecimal.ONE, BigDecimal.TEN, "RES", "NO", params);

        assertThat(sql).contains("capacityfrom");
        assertThat(params).containsExactly("pb", BigDecimal.ONE, BigDecimal.TEN, "RES", "NO");
    }

    @Test
    void getBillingSlabSearchQuery_withStateTenant_usesExactMatch() {
        when(config.getDefaultLimit()).thenReturn(100);
        when(config.getDefaultOffset()).thenReturn(0);

        List<Object> params = new ArrayList<>();
        BillingSlabSearchCriteria criteria = BillingSlabSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .propertyType("RES")
                .limit(10)
                .offset(0)
                .build();

        String sql = queryBuilder.getBillingSlabSearchQuery(criteria, params);

        assertThat(sql).contains("propertytype=?");
        assertThat(params.get(0)).isEqualTo("pb.amritsar");
    }
}
