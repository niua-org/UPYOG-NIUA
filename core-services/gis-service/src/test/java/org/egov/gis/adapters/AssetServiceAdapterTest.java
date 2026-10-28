package org.egov.gis.adapters;

import org.egov.common.contract.request.RequestInfo;
import org.egov.gis.config.GisConfiguration;
import org.egov.gis.models.Entity;
import org.egov.gis.models.GisRequest;
import org.egov.gis.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceAdapterTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private GisConfiguration config;

    private AssetServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AssetServiceAdapter(serviceRequestRepository, config);
    }

    @Test
    void getBusinessService_returnsAsset() {
        assertThat(adapter.getBusinessService()).isEqualTo("ASSET");
    }

    @Test
    void fetchEntities_emptyServiceResponse_returnsEmptyList() {
        when(config.getAssetHost()).thenReturn("http://localhost:8090/");
        when(config.getAssetSearchEndpoint()).thenReturn("asset-services/assets/_search");
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenReturn(Optional.of(Collections.emptyMap()));

        GisRequest request = GisRequest.builder()
                .tenantId("pg.citya")
                .requestInfo(RequestInfo.builder().build())
                .build();

        List<Entity> entities = adapter.fetchEntities(request);

        assertThat(entities).isEmpty();
    }

    @Test
    void fetchEntities_assetWithPointGeometry_mapsEntityFields() {
        when(config.getAssetHost()).thenReturn("http://localhost:8090/");
        when(config.getAssetSearchEndpoint()).thenReturn("asset-services/assets/_search");
        Map<String, Object> asset = new HashMap<>();
        asset.put("id", "A-1");
        asset.put("tenantId", "pg.citya");
        asset.put("status", "APPROVED");
        Map<String, Object> geometry = Map.of("type", "Point", "coordinates", List.of(74.8, 31.6));
        asset.put("pointGeometry", geometry);
        Map<String, Object> response = Map.of("Assets", List.of(asset));
        when(serviceRequestRepository.fetchResult(any(StringBuilder.class), any()))
                .thenReturn(Optional.of(response));

        GisRequest request = GisRequest.builder()
                .tenantId("pg.citya")
                .requestInfo(RequestInfo.builder().build())
                .build();

        List<Entity> entities = adapter.fetchEntities(request);

        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getId()).isEqualTo("A-1");
    }
}
