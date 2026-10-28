package org.egov.pt.repository.builder;

import org.egov.common.utils.MultiStateInstanceUtil;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.PropertyCriteria;
import org.egov.tracer.model.CustomException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PropertyQueryBuilderTest {

    @Mock
    private MultiStateInstanceUtil centralInstanceUtil;
    @Mock
    private PropertyConfiguration config;

    @InjectMocks
    private PropertyQueryBuilder queryBuilder;

    @Test
    void getPropertySearchQuery_whenCriteriaEmpty_throwsSearchError() {
        PropertyCriteria criteria = PropertyCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();

        assertThatThrownBy(
                        () -> queryBuilder.getPropertySearchQuery(criteria, params, false, false))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("No criteria given");
    }

    @Test
    void getPropertySearchQuery_whenToDateWithoutFromDate_throwsInvalidSearch() {
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .propertyIds(Set.of("prop-1"))
                .toDate(100L)
                .build();
        List<Object> params = new ArrayList<>();

        assertThatThrownBy(
                        () -> queryBuilder.getPropertySearchQuery(criteria, params, false, false))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("From Date should be mentioned first");
    }

    @Test
    void getPropertyIdsQuery_whenOwnerIdsGiven_bindsPlaceholdersAndTenant() {
        Set<String> ownerIds = new LinkedHashSet<>(List.of("u1", "u2"));
        List<Object> params = new ArrayList<>();

        String sql = queryBuilder.getPropertyIdsQuery(ownerIds, "pb.amritsar", params);

        assertThat(sql).contains("userid IN");
        assertThat(sql).contains("?");
        assertThat(params).containsExactly("u1", "u2", "pb.amritsar");
    }

    @Test
    void getTotalApplicationsCountQueryString_whenTenantGiven_substitutesTenant() {
        PropertyCriteria criteria = PropertyCriteria.builder().tenantId("pb.amritsar").build();

        String sql = queryBuilder.getTotalApplicationsCountQueryString(criteria);

        assertThat(sql).contains("pb.amritsar");
        assertThat(sql).contains("count(*)");
    }

    @Test
    void getpropertyAuditQuery_whenCalled_returnsAuditSelect() {
        assertThat(queryBuilder.getpropertyAuditQuery()).contains("eg_pt_property_audit");
    }
}
