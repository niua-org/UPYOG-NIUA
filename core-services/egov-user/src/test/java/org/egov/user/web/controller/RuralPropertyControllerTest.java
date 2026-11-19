package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.asset.RuralPropertyDetail;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.service.asset.RuralPropertyService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.RuralPropertyRequest;
import org.egov.user.web.contract.asset.RuralPropertyResponse;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying HTTP request handling, response serialization, and service delegation in {@link RuralPropertyController}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class RuralPropertyControllerTest {

    private RuralPropertyService ruralPropertyService;
    private RuralPropertyController controller;

    private static final String TEST_PROPERTY_ID = "prop-rural-001";
    private static final String TEST_USER_UUID = "citizen-uuid-001";

    @BeforeEach
    public void setUp() {
        ruralPropertyService = mock(RuralPropertyService.class);
        controller = new RuralPropertyController(ruralPropertyService);
    }

    @Test
    @DisplayName("Should return HTTP 201 CREATED when rural property is created successfully")
    public void testCreateRuralProperty_ReturnsCreated() {
        UserProperty createdProperty = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .localityType(UserConstants.LOCALITY_TYPE_RURAL)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .ruralPropertyDetail(RuralPropertyDetail.builder()
                        .gramPanchayat("Rampur")
                        .villageName("Fatehpur")
                        .build())
                .build();

        when(ruralPropertyService.createRuralProperty(any(RuralPropertyRequest.class))).thenReturn(createdProperty);

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId("pg.citya")
                .ruralProperty(RuralPropertyDetail.builder()
                        .gramPanchayat("Rampur")
                        .villageName("Fatehpur")
                        .build())
                .build();

        ResponseEntity<RuralPropertyResponse> response = controller.createRuralProperty(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getProperties().size());
        assertEquals(TEST_PROPERTY_ID, response.getBody().getProperties().get(0).getId());
        assertEquals(UserConstants.STATUS_SUCCESS, response.getBody().getResponseInfo().getStatus());
        verify(ruralPropertyService, times(1)).createRuralProperty(any(RuralPropertyRequest.class));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when rural property is updated successfully")
    public void testUpdateRuralProperty_ReturnsOk() {
        UserProperty updatedProperty = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(ruralPropertyService.updateRuralProperty(any(RuralPropertyRequest.class))).thenReturn(updatedProperty);

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .propertyId(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        ResponseEntity<RuralPropertyResponse> response = controller.updateRuralProperty(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getProperties().size());
        verify(ruralPropertyService, times(1)).updateRuralProperty(any(RuralPropertyRequest.class));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK with results when searching rural properties")
    public void testSearchRuralProperties_ReturnsOk() {
        UserProperty property = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(ruralPropertyService.searchRuralProperties(any(RuralPropertySearchCriteria.class), any()))
                .thenReturn(Collections.singletonList(property));

        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .build();

        ResponseEntity<RuralPropertyResponse> response = controller.searchRuralProperties(criteria);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalCount());
        assertEquals(TEST_PROPERTY_ID, response.getBody().getProperties().get(0).getId());
        verify(ruralPropertyService, times(1)).searchRuralProperties(any(RuralPropertySearchCriteria.class), any());
    }

    @Test
    @DisplayName("Should unwrap nested criteria during search if present")
    public void testSearchRuralProperties_NestedCriteriaUnwrapping() {
        RuralPropertySearchCriteria inner = RuralPropertySearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .build();
        RuralPropertySearchCriteria outer = RuralPropertySearchCriteria.builder()
                .requestInfo(new RequestInfo())
                .criteria(inner)
                .build();

        when(ruralPropertyService.searchRuralProperties(any(RuralPropertySearchCriteria.class), any()))
                .thenReturn(Collections.emptyList());

        ResponseEntity<RuralPropertyResponse> response = controller.searchRuralProperties(outer);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(ruralPropertyService, times(1)).searchRuralProperties(any(RuralPropertySearchCriteria.class), any());
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when deactivating rural property")
    public void testDeleteRuralProperty_ReturnsOk() {
        UserProperty deactivatedProperty = UserProperty.builder()
                .id(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .status(UserConstants.STATUS_INACTIVE_UPPERCASE)
                .build();

        when(ruralPropertyService.deleteRuralProperty(any(RuralPropertyRequest.class))).thenReturn(deactivatedProperty);

        RuralPropertyRequest request = RuralPropertyRequest.builder()
                .propertyId(TEST_PROPERTY_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        ResponseEntity<RuralPropertyResponse> response = controller.deleteRuralProperty(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(UserConstants.STATUS_INACTIVE_UPPERCASE, response.getBody().getProperties().get(0).getStatus());
        verify(ruralPropertyService, times(1)).deleteRuralProperty(any(RuralPropertyRequest.class));
    }
}
