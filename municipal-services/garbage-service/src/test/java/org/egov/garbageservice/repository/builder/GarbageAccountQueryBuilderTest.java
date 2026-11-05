package org.egov.garbageservice.repository.builder;

import org.egov.garbageservice.web.models.SearchCriteriaGarbageAccount;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GarbageAccountQueryBuilderTest {

    private final GarbageAccountQueryBuilder queryBuilder = new GarbageAccountQueryBuilder();

    @Test
    void getSearchQueryByCriteria_withTenantId_addsTenantPredicate() {
        SearchCriteriaGarbageAccount criteria = SearchCriteriaGarbageAccount.builder()
                .tenantId("pb.amritsar")
                .isSchedulerCall(true)
                .build();
        List<Object> params = new ArrayList<>();

        StringBuilder query = queryBuilder.getSearchQueryByCriteria(new StringBuilder(), criteria, params, new HashMap<>());

        assertThat(query.toString()).contains("WITH filtered_acc");
        assertThat(params).isNotEmpty();
    }

    @Test
    void staticSequenceQueries_areDefined() {
        assertThat(GarbageAccountQueryBuilder.SELECT_NEXT_SEQUENCE).contains("seq_id_udd_grbg_account");
        assertThat(GarbageAccountQueryBuilder.SELECT_NEXT_GARBAGE_ID).contains("seq_ug_grbg_account_id");
    }
}
