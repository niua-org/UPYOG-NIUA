package org.egov.swservice.repository.builder;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.swservice.config.SWConfiguration;
import org.egov.swservice.service.UserService;
import org.egov.swservice.util.SewerageServicesUtil;
import org.egov.swservice.web.models.SearchCriteria;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SWQueryBuilderTest {

    @Mock
    private SewerageServicesUtil sewerageServicesUtil;
    @Mock
    private SWConfiguration config;
    @Mock
    private UserService userService;
    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;

    @InjectMocks
    private SWQueryBuilder queryBuilder;

    @BeforeEach
    void stubPaginationDefaults() {
        when(config.getDefaultLimit()).thenReturn(10);
        when(config.getDefaultOffset()).thenReturn(0);
        when(config.getMaxLimit()).thenReturn(100);
    }

    @Test
    void getSearchQueryString_whenCriteriaEmpty_returnsNull() {
        SearchCriteria criteria = SearchCriteria.builder().build();
        List<Object> params = new ArrayList<>();

        assertThat(queryBuilder.getSearchQueryString(criteria, params, RequestInfo.builder().build())).isNull();
    }

    @Test
    void getSearchQueryString_whenTenantIdOnly_addsTenantPredicate() {
        when(centralInstanceUtil.isTenantIdStateLevel("pb")).thenReturn(true);
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSearchQueryString(criteria, params, RequestInfo.builder().build());

        assertThat(sql).contains("conn.tenantid LIKE ?");
        assertThat(params).contains("pb%");
        assertThat(params).contains(0, 100);
    }

    @Test
    void getSearchQueryString_whenUlbtTenant_usesEquality() {
        when(centralInstanceUtil.isTenantIdStateLevel("pb.amritsar")).thenReturn(false);
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSearchQueryString(criteria, params, RequestInfo.builder().build());

        assertThat(sql).contains("conn.tenantid = ?");
        assertThat(params).contains("pb.amritsar");
        assertThat(params).contains(0, 100);
    }

    @Test
    void getSearchQueryString_whenIdsGiven_bindsIdPlaceholders() {
        when(centralInstanceUtil.isTenantIdStateLevel("pb.amritsar")).thenReturn(false);
        Set<String> ids = new LinkedHashSet<>(List.of("id-1", "id-2"));
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").ids(ids).build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getSearchQueryString(criteria, params, RequestInfo.builder().build());

        assertThat(sql).contains("conn.id in");
        assertThat(params).contains("pb.amritsar", "id-1", "id-2");
    }

    @Test
    void getTotalApplicationsCountQueryString_substitutesTenant() {
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getTotalApplicationsCountQueryString(criteria, params);

        assertThat(sql).contains("count(*)");
        assertThat(params).containsExactly("pb.amritsar");
    }

    @Test
    void getSearchQueryStringForPlainSearch_whenEmpty_returnsNull() {
        assertThat(queryBuilder.getSearchQueryStringForPlainSearch(
                        SearchCriteria.builder().build(), new ArrayList<>(), RequestInfo.builder().build()))
                .isNull();
    }

    @Test
    void getLastExecutionDetail_addsTenantToPreparedStatement() {
        when(centralInstanceUtil.isTenantIdStateLevel("pb")).thenReturn(true);
        SearchCriteria criteria = SearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getLastExecutionDetail(criteria, params);

        assertThat(sql).contains("eg_sw_enc_audit");
        assertThat(params).containsExactly("pb%");
    }
}
