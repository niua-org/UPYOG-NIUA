package org.egov.pt.calculator.repository.querybuilder;

import org.egov.pt.calculator.web.models.BillingSlabSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BillingSlabQueryBuilderTest {

    private final BillingSlabQueryBuilder queryBuilder = new BillingSlabQueryBuilder();

    @Test
    void getBillingSlabSearchQuery_includesTenantAndOptionalFilters() {
        List<Object> params = new ArrayList<>();
        BillingSlabSearchCriteria criteria = BillingSlabSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .propertyType("RESIDENTIAL")
                .id(List.of("slab-1", "slab-2"))
                .build();

        String sql = queryBuilder.getBillingSlabSearchQuery(criteria, params);

        assertThat(sql).startsWith("SELECT * FROM eg_pt_billingslab_v2");
        assertThat(sql).contains("tenantId = ?");
        assertThat(sql).contains("propertyType = ?");
        assertThat(sql).contains("id IN");
        assertThat(params.subList(0, 4)).containsExactly("pb.amritsar", "slab-1", "slab-2", "RESIDENTIAL");
    }
}
