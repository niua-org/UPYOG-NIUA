package org.egov.gis.service;

import org.egov.gis.interfaces.MunicipalServiceAdapter;
import org.egov.gis.models.Entity;
import org.egov.gis.models.GisRequest;
import org.egov.gis.models.GisResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GisServiceTest {

    @Mock
    private ServiceRegistry serviceRegistry;

    @Mock
    private MunicipalServiceAdapter adapter;

    @InjectMocks
    private GisService gisService;

    @Test
    void getEntitiesWithPoints_missingPointGeometry_returnsResponseWithPoint() {
        GisRequest request = request("pb.amritsar", "PT");
        Map<String, Object> attributes = new HashMap<String, Object>();
        Entity entity = Entity.builder().id("prop-1").attributes(attributes).build();
        Map<String, Object> point = new HashMap<String, Object>();
        point.put("type", "Point");
        point.put("longitude", Double.valueOf(75.8));
        point.put("latitude", Double.valueOf(31.6));

        when(serviceRegistry.getAdapter("PT")).thenReturn(adapter);
        when(adapter.fetchEntities(request)).thenReturn(Collections.singletonList(entity));
        when(adapter.extractPointGeometry(entity)).thenReturn(point);

        GisResponse response = gisService.getEntitiesWithPoints(request);

        assertThat(response.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(response.getBusinessService()).isEqualTo("PT");
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getEntities()).containsExactly(entity);
        assertThat(entity.getAttributes()).containsEntry("pointGeometry", point);
    }

    @Test
    void getEntitiesWithPolygons_emptyEntityList_returnsZeroTotalCount() {
        GisRequest request = request("pb.amritsar", "PT");
        when(serviceRegistry.getAdapter("PT")).thenReturn(adapter);
        when(adapter.fetchEntities(request)).thenReturn(Collections.<Entity>emptyList());

        GisResponse response = gisService.getEntitiesWithPolygons(request);

        assertThat(response.getEntities()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);
        assertThat(response.getTenantId()).isEqualTo("pb.amritsar");
        verify(adapter, never()).extractPolygonGeometry(any(Entity.class));
    }

    @Test
    void getEntitiesWithPoints_nullPointGeometry_omitsPointAttribute() {
        GisRequest request = request("pb.amritsar", "PT");
        Map<String, Object> attributes = new HashMap<String, Object>();
        Entity entity = Entity.builder().id("prop-2").attributes(attributes).build();

        when(serviceRegistry.getAdapter("PT")).thenReturn(adapter);
        when(adapter.fetchEntities(request)).thenReturn(Collections.singletonList(entity));
        when(adapter.extractPointGeometry(entity)).thenReturn(null);

        GisResponse response = gisService.getEntitiesWithPoints(request);

        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(entity.getAttributes()).doesNotContainKey("pointGeometry");
    }

    @Test
    void getEntitiesWithPoints_unknownBusinessService_throwsIllegalArgumentException() {
        GisRequest request = request("pb.amritsar", "UNKNOWN");
        when(serviceRegistry.getAdapter("UNKNOWN"))
                .thenThrow(new IllegalArgumentException("No adapter found for business service: UNKNOWN"));

        assertThatThrownBy(() -> gisService.getEntitiesWithPoints(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void getAvailableBusinessServices_registryReturnsNames_returnsSameNames() {
        List<String> services = Arrays.asList("PT", "TL");
        when(serviceRegistry.getAvailableBusinessServices()).thenReturn(services);

        assertThat(gisService.getAvailableBusinessServices()).containsExactly("PT", "TL");
    }

    private static GisRequest request(String tenantId, String businessService) {
        return GisRequest.builder()
                .tenantId(tenantId)
                .businessService(businessService)
                .build();
    }
}
