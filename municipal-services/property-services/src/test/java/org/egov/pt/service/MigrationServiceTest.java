package org.egov.pt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.models.oldProperty.MigrationCount;
import org.egov.pt.models.oldProperty.OldOwnerInfo;
import org.egov.pt.models.oldProperty.OldProperty;
import org.egov.pt.models.oldProperty.OldPropertyCriteria;
import org.egov.pt.models.oldProperty.OldPropertyResponse;
import org.egov.pt.models.oldProperty.PropertyDetail;
import org.egov.pt.repository.builder.OldPropertyQueryBulider;
import org.egov.pt.repository.rowmapper.MigrationCountRowMapper;
import org.egov.pt.repository.rowmapper.OldPropertyRowMapper;
import org.egov.pt.web.contracts.RequestInfoWrapper;
import org.egov.tracer.model.ServiceCallException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationServiceTest {

    @Mock private org.egov.pt.producer.PropertyProducer producer;
    @Mock private org.egov.pt.validator.AssessmentValidator validator;
    @Mock private org.egov.pt.config.PropertyConfiguration config;
    @Mock private org.egov.pt.validator.PropertyMigrationValidator propertyMigrationValidator;
    @Mock private org.egov.pt.util.AssessmentUtils assmtUtils;
    @Mock private org.egov.pt.repository.AssessmentRepository assessmentRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private OldPropertyQueryBulider queryBuilder;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private OldPropertyRowMapper rowMapper;
    @Mock private MigrationCountRowMapper migrationCountRowMapper;
    @Mock private org.egov.pt.repository.ServiceRequestRepository restRepo;

    @InjectMocks
    private MigrationService migrationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void injectFields() {
        ReflectionTestUtils.setField(migrationService, "mapper", objectMapper);
        ReflectionTestUtils.setField(migrationService, "ptHost", "http://pt-host");
        ReflectionTestUtils.setField(migrationService, "oldPropertySearchEndpoint", "/property/_search");
        ReflectionTestUtils.setField(migrationService, "userHost", "http://user");
        ReflectionTestUtils.setField(migrationService, "userSearchEndpoint", "/user/_search");
        ReflectionTestUtils.setField(migrationService, "batchSize", 100);
        ReflectionTestUtils.setField(migrationService, "batchOffset", 0);
    }

    @Test
    void getTenantCount_whenJdbcReturnsValue_returnsLongCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(25);

        long count = migrationService.getTenantCount("pb.amritsar");

        assertThat(count).isEqualTo(25L);
        verify(jdbcTemplate).queryForObject(eq("select count(*) from eg_pt_property_v2 where tenantid = 'pb.amritsar';"),
                eq(Integer.class));
    }

    @Test
    void getTenantList_whenJdbcReturnsRows_returnsTenantIds() {
        when(jdbcTemplate.queryForList(MigrationService.TENANT_QUERY, String.class))
                .thenReturn(List.of("pb.amritsar", "pb.ludhiana"));

        List<String> tenants = migrationService.getTenantList();

        assertThat(tenants).containsExactly("pb.amritsar", "pb.ludhiana");
    }

    @Test
    void getMigrationCountForTenant_whenQueryRuns_returnsMigrationCount() {
        MigrationCount expected = MigrationCount.builder().id("m1").offset(0L).limit(100L).recordCount(50L).build();
        when(jdbcTemplate.query(eq(MigrationService.MIGARTION_POINT_QUERY), any(Object[].class), eq(migrationCountRowMapper)))
                .thenReturn(expected);

        MigrationCount result = migrationService.getMigrationCountForTenant("pb.amritsar");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getPropertiesPlainSearch_whenJdbcReturnsRows_returnsProperties() {
        OldPropertyCriteria criteria = OldPropertyCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> stmt = new ArrayList<>();
        when(queryBuilder.getPropertyLikeQuery(criteria, stmt, 0L)).thenReturn("SELECT * FROM old");
        OldProperty oldProperty = new OldProperty();
        when(jdbcTemplate.query(eq("SELECT * FROM old"), any(Object[].class), eq(rowMapper)))
                .thenReturn(List.of(oldProperty));

        List<OldProperty> result = migrationService.getPropertiesPlainSearch(criteria, 0L);

        assertThat(result).containsExactly(oldProperty);
    }

    @Test
    void enrichPropertyCriteriaWithOwnerids_whenPropertyDetailsPresent_collectsOwnerUuids() {
        OldOwnerInfo owner = new OldOwnerInfo();
        owner.setUuid("owner-uuid-1");
        OldOwnerInfo citizen = new OldOwnerInfo();
        citizen.setUuid("citizen-uuid-1");
        PropertyDetail detail = PropertyDetail.builder()
                .owners(new HashSet<>(Set.of(owner)))
                .citizenInfo(citizen)
                .build();
        OldProperty property = new OldProperty();
        property.setPropertyDetails(List.of(detail));
        OldPropertyCriteria criteria = OldPropertyCriteria.builder().build();

        migrationService.enrichPropertyCriteriaWithOwnerids(criteria, List.of(property));

        assertThat(criteria.getOwnerids()).contains("owner-uuid-1", "citizen-uuid-1");
    }

    @Test
    void searchOldPropertyFromURL_whenFetchSucceeds_returnsPropertiesFromResponse() {
        RequestInfoWrapper wrapper = RequestInfoWrapper.builder()
                .requestInfo(RequestInfo.builder().build())
                .build();
        OldPropertyCriteria criteria = OldPropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .offset(0L)
                .limit(10L)
                .build();
        Map<String, Object> apiBody = new HashMap<>();
        apiBody.put("properties", List.of());
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenReturn(apiBody);

        List<OldProperty> properties = migrationService.searchOldPropertyFromURL(wrapper, criteria);

        assertThat(properties).isNotNull();
    }

    @Test
    void fetchResult_whenHttpClientError_throwsServiceCallException() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "bad",
                org.springframework.http.HttpHeaders.EMPTY,
                "error-body".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.postForObject(anyString(), any(), eq(Map.class))).thenThrow(ex);

        assertThatThrownBy(() -> migrationService.fetchResult(new StringBuilder("http://x"), RequestInfo.builder().build()))
                .isInstanceOf(ServiceCallException.class);
    }

    @Test
    void searchPropertyPlainSearch_delegatesToJdbcQuery() {
        OldPropertyCriteria criteria = OldPropertyCriteria.builder().tenantId("pb.amritsar").build();
        when(queryBuilder.getPropertyLikeQuery(eq(criteria), any(), eq(5L))).thenReturn("Q");
        when(jdbcTemplate.query(eq("Q"), any(Object[].class), eq(rowMapper))).thenReturn(List.of(new OldProperty()));

        List<OldProperty> result = migrationService.searchPropertyPlainSearch(criteria, RequestInfo.builder().build(), 5L);

        assertThat(result).hasSize(1);
    }
}
