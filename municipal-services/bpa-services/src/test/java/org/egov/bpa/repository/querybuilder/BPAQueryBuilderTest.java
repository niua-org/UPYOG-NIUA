package org.egov.bpa.repository.querybuilder;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BPAQueryBuilderTest {

    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private BPAConfiguration config;

    @InjectMocks
    private BPAQueryBuilder queryBuilder;

    @BeforeEach
    void stubPaginationDefaults() {
        when(config.getDefaultLimit()).thenReturn(10);
        when(config.getDefaultOffset()).thenReturn(0);
        when(config.getMaxSearchLimit()).thenReturn(100);
    }

    @Test
    void getBPASearchQuery_whenStateLevelTenant_usesLikePredicate() {
        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getBPASearchQuery(criteria, params, List.of(), false);

        assertThat(sql).contains("bpa.tenantid like ?");
        assertThat(params).contains("%pb%");
        assertThat(params).contains(0, 10);
    }

    @Test
    void getBPASearchQuery_whenUlbtTenant_usesEquality() {
        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getBPASearchQuery(criteria, params, List.of(), false);

        assertThat(sql).contains("bpa.tenantid=?");
        assertThat(params).contains("pb.amritsar");
    }

    @Test
    void getBPASearchQuery_whenIdsProvided_addsInClause() {
        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").ids(List.of("id-1", "id-2")).build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getBPASearchQuery(criteria, params, List.of(), false);

        assertThat(sql).contains("bpa.id IN ( ?, ?)");
        assertThat(params).contains("id-1", "id-2");
    }

    @Test
    void getBPASearchQuery_whenCountRequested_wrapsWithCountQuery() {
        BPASearchCriteria criteria = BPASearchCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getBPASearchQuery(criteria, params, List.of(), true);

        assertThat(sql).startsWith("SELECT COUNT(DISTINCT(bpa_id))");
    }
}
