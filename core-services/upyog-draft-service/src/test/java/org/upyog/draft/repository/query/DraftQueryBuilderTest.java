package org.upyog.draft.repository.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.upyog.draft.util.DraftConstants;
import org.upyog.draft.web.models.DraftSearchCriteria;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DraftQueryBuilderTest {

    private DraftQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new DraftQueryBuilder();
    }

    @Test
    void getSearchQuery_allCriteriaPresent_buildsWhereOrderAndPagination() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .createdBy("user-1")
                .businessService("PT")
                .moduleName("property")
                .creatorType("USER")
                .status(DraftConstants.STATUS_ACTIVE)
                .sortBy("lastModifiedTime")
                .sortOrder("ASC")
                .offset(5)
                .limit(20)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSearchQuery(criteria, params);

        assertThat(sql).contains("FROM ug_draft_detail WHERE 1=1");
        assertThat(sql).contains("tenant_id = ?");
        assertThat(sql).contains("createdby = ?");
        assertThat(sql).contains("business_service = ?");
        assertThat(sql).contains("module_name = ?");
        assertThat(sql).contains("creator_type = ?");
        assertThat(sql).contains("status = ?");
        assertThat(sql).contains("ORDER BY lastmodifiedtime ASC");
        assertThat(sql).endsWith("OFFSET ? LIMIT ?");
        assertThat(params).containsExactly(
                "pb.amritsar", "user-1", "PT", "property", "USER", DraftConstants.STATUS_ACTIVE, 5, 20);
    }

    @Test
    void getSearchQuery_minimalCriteria_usesDefaultSortAndPagination() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder().build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSearchQuery(criteria, params);

        assertThat(sql).contains("ORDER BY lastmodifiedtime DESC");
        assertThat(sql).endsWith("OFFSET ? LIMIT ?");
        assertThat(params).containsExactly(0, 10);
    }

    @Test
    void getCountQuery_withTenantAndStatus_doesNotAppendPagination() {
        DraftSearchCriteria criteria = DraftSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .status(DraftConstants.STATUS_DISCARDED)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getCountQuery(criteria, params);

        assertThat(sql).startsWith("SELECT COUNT(*) FROM ug_draft_detail WHERE 1=1");
        assertThat(sql).doesNotContain("ORDER BY");
        assertThat(sql).doesNotContain("OFFSET");
        assertThat(params).containsExactly("pb.amritsar", DraftConstants.STATUS_DISCARDED);
    }

    @Test
    void activeTtlParams_returnsSingleCutoffParameter() {
        List<Object> params = queryBuilder.activeTtlParams(1_700_000_000_000L);

        assertThat(params).containsExactly(1_700_000_000_000L);
    }
}
