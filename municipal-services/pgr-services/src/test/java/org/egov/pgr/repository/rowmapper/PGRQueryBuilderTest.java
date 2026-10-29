package org.egov.pgr.repository.rowmapper;

import org.egov.pgr.config.PGRConfiguration;
import org.egov.pgr.web.models.RequestSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PGRQueryBuilderTest {

    @Mock
    private PGRConfiguration config;

    @InjectMocks
    private PGRQueryBuilder queryBuilder;

    @BeforeEach
    void stubStateTenantLength() {
        when(config.getStateLevelTenantIdLength()).thenReturn(1);
    }

    @Test
    void getPGRSearchQuery_whenStateTenant_usesLikePredicate() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg")
                .offset(0)
                .limit(10)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getPGRSearchQuery(criteria, params);

        assertThat(sql).contains("ser.tenantid LIKE ?");
        assertThat(params).contains("pg%");
        assertThat(params).contains(0, 10);
    }

    @Test
    void getPGRSearchQuery_whenUlbtTenant_usesEquality() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg.citya")
                .offset(0)
                .limit(5)
                .build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getPGRSearchQuery(criteria, params);

        assertThat(sql).contains("ser.tenantid=?");
        assertThat(params).contains("pg.citya");
    }

    @Test
    void getPGRSearchQuery_whenPlainSearch_bindsTenantIdSet() {
        Set<String> tenantIds = new LinkedHashSet<>(List.of("pg.citya", "pg.cityb"));
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .offset(0)
                .limit(10)
                .build();
        criteria.setIsPlainSearch(true);
        criteria.setTenantIds(tenantIds);
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getPGRSearchQuery(criteria, params);

        assertThat(sql).contains("ser.tenantId IN");
        assertThat(params).contains("pg.citya", "pg.cityb");
    }

    @Test
    void getPGRSearchQuery_whenToDateWithoutFromDate_throwsInvalidSearch() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg.citya")
                .toDate(1_700_000_000_000L)
                .offset(0)
                .limit(10)
                .build();
        List<Object> params = new ArrayList<>();

        assertThatThrownBy(() -> queryBuilder.getPGRSearchQuery(criteria, params))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Cannot specify to-Date without a from-Date");
    }

    @Test
    void getCountQuery_wrapsSearchQuery() {
        RequestSearchCriteria criteria = RequestSearchCriteria.builder()
                .tenantId("pg.citya")
                .offset(0)
                .limit(10)
                .build();
        List<Object> params = new ArrayList<>();

        String countSql = queryBuilder.getCountQuery(criteria, params);

        assertThat(countSql).startsWith("select count(*) from (");
        assertThat(countSql).endsWith(") as count");
    }

    @Test
    void getResolvedComplaints_bindsTenantAndCutoff() {
        when(config.getNumberOfDays()).thenReturn("365");
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getResolvedComplaints("pg.citya", params);

        assertThat(sql).contains("CLOSEDAFTERRESOLUTION");
        assertThat(params).hasSize(2);
        assertThat(params.get(0)).isEqualTo("pg.citya");
        assertThat(params.get(1)).isInstanceOf(Long.class);
    }
}
