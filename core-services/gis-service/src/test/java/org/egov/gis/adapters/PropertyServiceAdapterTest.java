package org.egov.gis.adapters;

import org.egov.gis.config.GisConfiguration;
import org.egov.gis.models.Entity;
import org.egov.gis.models.GisRequest;
import org.egov.gis.repository.ServiceRequestRepository;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceAdapterTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private GisConfiguration config;

    private PropertyServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PropertyServiceAdapter(serviceRequestRepository, config);
    }

    @Test
    void getBusinessService_always_returnsPt() {
        assertThat(adapter.getBusinessService()).isEqualTo("PT");
    }

    @Test
    void getServiceEndpoint_configuredHostAndPath_concatenatesBoth() {
        when(config.getPropertyHost()).thenReturn("http://localhost:8280/");
        when(config.getPropertySearchEndpoint()).thenReturn("property-services/property/_search");

        assertThat(adapter.getServiceEndpoint())
                .isEqualTo("http://localhost:8280/property-services/property/_search");
    }

    @Test
    void fetchEntities_propertyWithCoordinates_returnsEntityWithPointGeometry() {
        stubEndpoint();
        Map<String, Object> property = property("PT-1", 31.63, 74.87);
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenReturn(Optional.of(responseWith(property)));

        List<Entity> entities = adapter.fetchEntities(request(null, null, null));

        assertThat(entities).hasSize(1);
        Entity entity = entities.get(0);
        assertThat(entity.getId()).isEqualTo("PT-1");
        assertThat(entity.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(entity.getBusinessService()).isEqualTo("PT");
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        assertThat(entity.getAttributes().get("propertyType")).isEqualTo("BUILTUP");
        @SuppressWarnings("unchecked")
        Map<String, Object> point = (Map<String, Object>) entity.getAttributes().get("pointGeometry");
        assertThat(point.get("type")).isEqualTo("Point");
        assertThat(point.get("coordinates")).isEqualTo(java.util.Arrays.asList(74.87, 31.63));

        ArgumentCaptor<StringBuilder> uri = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(uri.capture(), any());
        assertThat(uri.getValue().toString()).contains("tenantId=pb.amritsar").contains("status=ACTIVE");
    }

    @Test
    void fetchEntities_emptyRepositoryResponse_returnsEmptyList() {
        stubEndpoint();
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenReturn(Optional.empty());

        assertThat(adapter.fetchEntities(request(null, 1000L, 2000L))).isEmpty();
    }

    @Test
    void fetchEntities_responseWithoutProperties_returnsEmptyList() {
        stubEndpoint();
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenReturn(Optional.of(new HashMap<String, Object>()));

        assertThat(adapter.fetchEntities(request(null, null, null))).isEmpty();
    }

    @Test
    void fetchEntities_searchCriteriaPresent_skipsDefaultStatusAndDates() {
        stubEndpoint();
        Map<String, Object> criteria = new HashMap<String, Object>();
        criteria.put("propertyType", "BUILTUP");
        criteria.put("tenantId", "ignored-tenant");
        criteria.put("locality", null);
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenReturn(Optional.of(responseWith()));

        adapter.fetchEntities(request(criteria, 1000L, 2000L));

        ArgumentCaptor<StringBuilder> uri = ArgumentCaptor.forClass(StringBuilder.class);
        verify(serviceRequestRepository).fetchResult(uri.capture(), any());
        String called = uri.getValue().toString();
        assertThat(called).contains("propertyType=BUILTUP");
        assertThat(called).contains("tenantId=pb.amritsar");
        assertThat(called).doesNotContain("status=ACTIVE");
        assertThat(called).doesNotContain("fromDate");
        assertThat(called).doesNotContain("ignored-tenant");
        assertThat(called).doesNotContain("locality");
    }

    @Test
    void fetchEntities_repositoryThrowsCustomException_rethrowsSameException() {
        stubEndpoint();
        CustomException downstream = new CustomException("DOWNSTREAM", "property down");
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any())).thenThrow(downstream);

        assertThatThrownBy(() -> adapter.fetchEntities(request(null, null, null)))
                .isSameAs(downstream);
    }

    @Test
    void fetchEntities_repositoryThrowsRuntimeException_wrapsAsCustomException() {
        stubEndpoint();
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenThrow(new IllegalStateException("socket closed"));

        assertThatThrownBy(() -> adapter.fetchEntities(request(null, null, null)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("socket closed");
    }

    @Test
    void fetchEntities_nullRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> adapter.fetchEntities(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void extractPointGeometry_attributePresent_returnsMap() {
        Map<String, Object> point = new HashMap<String, Object>();
        point.put("type", "Point");
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("pointGeometry", point);
        Entity entity = Entity.builder().attributes(attributes).build();

        assertThat(adapter.extractPointGeometry(entity)).isSameAs(point);
    }

    @Test
    void extractPointGeometry_attributeMissing_returnsNull() {
        Entity entity = Entity.builder().attributes(new HashMap<String, Object>()).build();

        assertThat(adapter.extractPointGeometry(entity)).isNull();
    }

    @Test
    void extractPointGeometry_nullAttributes_throwsNullPointerException() {
        Entity entity = Entity.builder().build();

        assertThatThrownBy(() -> adapter.extractPointGeometry(entity))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void extractPolygonGeometry_attributePresent_returnsMap() {
        Map<String, Object> polygon = new HashMap<String, Object>();
        polygon.put("type", "Polygon");
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("polygonGeometry", polygon);
        Entity entity = Entity.builder().attributes(attributes).build();

        assertThat(adapter.extractPolygonGeometry(entity)).isSameAs(polygon);
    }

    @Test
    void extractPolygonGeometry_attributeMissing_returnsNull() {
        Entity entity = Entity.builder().attributes(new HashMap<String, Object>()).build();

        assertThat(adapter.extractPolygonGeometry(entity)).isNull();
    }

    @Test
    void transformToGenericEntity_addressWithoutCoordinates_omitsPointGeometry() {
        Map<String, Object> property = property("PT-2", null, null);

        Entity entity = adapter.transformToGenericEntity(property);

        assertThat(entity.getId()).isEqualTo("PT-2");
        assertThat(entity.getAttributes()).doesNotContainKey("pointGeometry");
        assertThat(entity.getAttributes()).doesNotContainKey("polygonGeometry");
    }

    @Test
    void transformToGenericEntity_stringCoordinates_parsesPointGeometry() {
        Map<String, Object> property = property("PT-3", null, null);
        Map<String, Object> geoLocation = new HashMap<String, Object>();
        geoLocation.put("latitude", "31.5");
        geoLocation.put("longitude", "75.1");
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) property.get("address");
        address.put("geoLocation", geoLocation);

        Entity entity = adapter.transformToGenericEntity(property);

        @SuppressWarnings("unchecked")
        Map<String, Object> point = (Map<String, Object>) entity.getAttributes().get("pointGeometry");
        assertThat(point.get("coordinates")).isEqualTo(java.util.Arrays.asList(75.1d, 31.5d));
    }

    @Test
    void getSearchCriteriaMapping_always_returnsPropertyFields() {
        assertThat(adapter.getSearchCriteriaMapping())
                .containsEntry("propertyType", "propertyType")
                .containsEntry("locality", "locality")
                .containsEntry("status", "status")
                .containsEntry("ownershipCategory", "ownershipCategory")
                .containsEntry("usageCategory", "usageCategory");
    }

    private void stubEndpoint() {
        when(config.getPropertyHost()).thenReturn("http://localhost:8280/");
        when(config.getPropertySearchEndpoint()).thenReturn("property-services/property/_search");
    }

    private static GisRequest request(Map<String, Object> searchCriteria, Long fromDate, Long toDate) {
        return GisRequest.builder()
                .tenantId("pb.amritsar")
                .businessService("PT")
                .searchCriteria(searchCriteria)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
    }

    private static Map<String, Object> responseWith(Map<String, Object>... properties) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("Properties", java.util.Arrays.asList(properties));
        return response;
    }

    private static Map<String, Object> property(String propertyId, Double latitude, Double longitude) {
        Map<String, Object> property = new HashMap<String, Object>();
        property.put("propertyId", propertyId);
        property.put("tenantId", "pb.amritsar");
        property.put("status", "ACTIVE");
        property.put("propertyType", "BUILTUP");
        property.put("locality", "Ward 1");
        property.put("createdTime", 10);
        property.put("createdBy", "user");
        Map<String, Object> address = new HashMap<String, Object>();
        if (latitude != null && longitude != null) {
            Map<String, Object> geoLocation = new HashMap<String, Object>();
            geoLocation.put("latitude", latitude);
            geoLocation.put("longitude", longitude);
            address.put("geoLocation", geoLocation);
        }
        property.put("address", address);
        return property;
    }
}
