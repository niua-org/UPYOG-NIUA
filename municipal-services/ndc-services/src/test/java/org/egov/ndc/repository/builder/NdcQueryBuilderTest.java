package org.egov.ndc.repository.builder;

import org.egov.ndc.config.NDCConfiguration;
import org.egov.ndc.web.model.ndc.NdcApplicationSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NdcQueryBuilderTest {

    @Mock
    private NDCConfiguration ndcConfig;

    private NdcQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new NdcQueryBuilder(ndcConfig);
    }

    private void stubPaginationDefaults() {
        when(ndcConfig.getDefaultLimit()).thenReturn(10);
        when(ndcConfig.getDefaultOffset()).thenReturn(0);
    }

    @Test
    void getPaginatedApplicationUuids_withTenantAndStatus_addsFiltersAndPagination() {
        stubPaginationDefaults();
        NdcApplicationSearchCriteria criteria = NdcApplicationSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .status("APPROVED")
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getPaginatedApplicationUuids(criteria, params);

        assertThat(sql).contains("WHERE");
        assertThat(sql).contains("a.tenantid = ?");
        assertThat(sql).contains("a.applicationstatus = ?");
        assertThat(sql).contains("LIMIT ? OFFSET ?");
        assertThat(params).containsExactly("pb.amritsar", "APPROVED", 10, 0);
    }

    @Test
    void getNdcApplicationDetailsQuery_whenUuidsEmpty_returnsNull() {
        assertThat(queryBuilder.getNdcApplicationDetailsQuery(List.of(), new ArrayList<>())).isNull();
    }

    @Test
    void getNdcApplicationDetailsQuery_whenUuidsPresent_buildsInClause() {
        List<Object> params = new ArrayList<>();
        String sql = queryBuilder.getNdcApplicationDetailsQuery(List.of("uuid-1", "uuid-2"), params);

        assertThat(sql).contains("WHERE a.uuid IN (?,?)");
        assertThat(params).containsExactly("uuid-1", "uuid-2");
    }

    @Test
    void getPaginatedApplicationUuids_withOwnerIds_addsOwnerSubquery() {
        stubPaginationDefaults();
        NdcApplicationSearchCriteria criteria = NdcApplicationSearchCriteria.builder()
                .tenantId("pb")
                .ownerIds(Set.of("owner-1"))
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getPaginatedApplicationUuids(criteria, params);

        assertThat(sql).contains("eg_ndc_owner");
        assertThat(params).contains("owner-1");
    }

    @Test
    void checkApplicationExists_returnsParameterizedLookup() {
        assertThat(queryBuilder.checkApplicationExists()).contains("eg_ndc_application");
    }
}
