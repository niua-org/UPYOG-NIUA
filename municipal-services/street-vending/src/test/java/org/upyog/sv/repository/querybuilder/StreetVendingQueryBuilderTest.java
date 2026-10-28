package org.upyog.sv.repository.querybuilder;

import org.junit.jupiter.api.Test;
import org.upyog.sv.web.models.StreetVendingSearchCriteria;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreetVendingQueryBuilderTest {

    private final StreetVendingQueryBuilder queryBuilder = new StreetVendingQueryBuilder();

    @Test
    void getStreetVendingSearchQuery_withTenantId_addsTenantPredicate() {
        StreetVendingSearchCriteria criteria = StreetVendingSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .build();
        criteria.setCountCall(true);
        List<Object> params = new ArrayList<>();

        String query = queryBuilder.getStreetVendingSearchQuery(criteria, params);

        assertThat(query).contains("sv.tenant_id LIKE ?");
        assertThat(params).contains("%pb.amritsar%");
    }
}
