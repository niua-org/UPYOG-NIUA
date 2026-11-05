package org.egov.gis.web.controller;

import org.egov.gis.models.GisRequest;
import org.egov.gis.models.GisResponse;
import org.egov.gis.service.GisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GisControllerTest {

    @Mock
    private GisService gisService;

    @InjectMocks
    private GisController gisController;

    @Test
    void searchEntitiesWithPoints_validRequest_returnsOkAndSetsPointGeometryType() {
        GisRequest request = request("pb.amritsar");
        GisResponse response = response("PT");
        when(gisService.getEntitiesWithPoints(request)).thenReturn(response);

        ResponseEntity<GisResponse> result = gisController.searchEntitiesWithPoints("PT", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        assertThat(request.getBusinessService()).isEqualTo("PT");
        assertThat(request.getGeometryType()).isEqualTo("point");
    }

    @Test
    void searchEntitiesWithPoints_emptyServiceResult_returnsOkWithZeroCount() {
        GisRequest request = request("pb.amritsar");
        GisResponse response = GisResponse.builder()
                .tenantId("pb.amritsar")
                .businessService("PT")
                .totalCount(0)
                .entities(Collections.emptyList())
                .build();
        when(gisService.getEntitiesWithPoints(request)).thenReturn(response);

        ResponseEntity<GisResponse> result = gisController.searchEntitiesWithPoints("PT", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getTotalCount()).isZero();
        assertThat(result.getBody().getEntities()).isEmpty();
    }

    @Test
    void searchEntitiesWithPoints_serviceThrows_returnsInternalServerError() {
        GisRequest request = request("pb.amritsar");
        when(gisService.getEntitiesWithPoints(request)).thenThrow(new IllegalArgumentException("No adapter found"));

        ResponseEntity<GisResponse> result = gisController.searchEntitiesWithPoints("PT", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isNull();
        assertThat(request.getGeometryType()).isEqualTo("point");
    }

    @Test
    void searchEntitiesWithPoints_nullRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> gisController.searchEntitiesWithPoints("PT", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void searchEntitiesWithPolygons_validRequest_returnsOkAndSetsPolygonGeometryType() {
        GisRequest request = request("pb.amritsar");
        GisResponse response = response("PT");
        when(gisService.getEntitiesWithPolygons(request)).thenReturn(response);

        ResponseEntity<GisResponse> result = gisController.searchEntitiesWithPolygons("PT", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        assertThat(request.getBusinessService()).isEqualTo("PT");
        assertThat(request.getGeometryType()).isEqualTo("polygon");
    }

    @Test
    void searchEntitiesWithPolygons_emptyServiceResult_returnsOkWithEmptyEntities() {
        GisRequest request = request("pb.amritsar");
        GisResponse response = GisResponse.builder()
                .tenantId("pb.amritsar")
                .businessService("PT")
                .totalCount(0)
                .entities(Collections.emptyList())
                .build();
        when(gisService.getEntitiesWithPolygons(request)).thenReturn(response);

        ResponseEntity<GisResponse> result = gisController.searchEntitiesWithPolygons("PT", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getEntities()).isEmpty();
    }

    @Test
    void searchEntitiesWithPolygons_serviceThrows_returnsInternalServerError() {
        GisRequest request = request("pb.amritsar");
        when(gisService.getEntitiesWithPolygons(request)).thenThrow(new IllegalStateException("downstream"));

        ResponseEntity<GisResponse> result = gisController.searchEntitiesWithPolygons("PT", request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isNull();
    }

    @Test
    void searchEntitiesWithPolygons_nullRequest_throwsNullPointerException() {
        assertThatThrownBy(() -> gisController.searchEntitiesWithPolygons("PT", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getAvailableBusinessServices_serviceReturnsIds_returnsOk() {
        when(gisService.getAvailableBusinessServices()).thenReturn(Collections.singletonList("PT"));

        ResponseEntity<List<String>> result = gisController.getAvailableBusinessServices();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly("PT");
    }

    @Test
    void getAvailableBusinessServices_serviceReturnsEmpty_returnsOkWithEmptyList() {
        when(gisService.getAvailableBusinessServices()).thenReturn(Collections.<String>emptyList());

        ResponseEntity<List<String>> result = gisController.getAvailableBusinessServices();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEmpty();
    }

    @Test
    void getAvailableBusinessServices_serviceReturnsNull_returnsOkWithNullBody() {
        when(gisService.getAvailableBusinessServices()).thenReturn(null);

        ResponseEntity<List<String>> result = gisController.getAvailableBusinessServices();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNull();
    }

    @Test
    void getAvailableBusinessServices_serviceThrows_returnsInternalServerError() {
        when(gisService.getAvailableBusinessServices()).thenThrow(new IllegalStateException("registry down"));

        ResponseEntity<List<String>> result = gisController.getAvailableBusinessServices();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isNull();
    }

    private static GisRequest request(String tenantId) {
        return GisRequest.builder()
                .tenantId(tenantId)
                .businessService("ignored")
                .build();
    }

    private static GisResponse response(String businessService) {
        return GisResponse.builder()
                .tenantId("pb.amritsar")
                .businessService(businessService)
                .totalCount(1)
                .entities(Collections.emptyList())
                .build();
    }
}
