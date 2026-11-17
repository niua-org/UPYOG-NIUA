package org.egov.user.web.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.service.asset.VehicleService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.VehicleRequest;
import org.egov.user.web.contract.asset.VehicleResponse;
import org.egov.user.web.contract.asset.VehicleSearchCriteria;
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
 * Unit tests verifying HTTP request handling, response serialization, and service delegation in {@link VehicleController}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class VehicleControllerTest {

    private VehicleService vehicleService;
    private VehicleController controller;

    private static final String TEST_VEHICLE_ID = "veh-001";
    private static final String TEST_USER_UUID = "citizen-uuid-001";

    @BeforeEach
    public void setUp() {
        vehicleService = mock(VehicleService.class);
        controller = new VehicleController(vehicleService);
    }

    @Test
    @DisplayName("Should return HTTP 201 CREATED when vehicle is created successfully")
    public void testCreateVehicle_ReturnsCreated() {
        UserVehicle createdVehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(vehicleService.createVehicle(any(VehicleRequest.class))).thenReturn(createdVehicle);

        VehicleRequest request = VehicleRequest.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId("pg.citya")
                .vehicleNumber("DL01AB1234")
                .vehicleType("4_WHEELER")
                .build();

        ResponseEntity<VehicleResponse> response = controller.createVehicle(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getVehicles().size());
        assertEquals(TEST_VEHICLE_ID, response.getBody().getVehicles().get(0).getId());
        assertEquals(UserConstants.STATUS_SUCCESS, response.getBody().getResponseInfo().getStatus());
        verify(vehicleService, times(1)).createVehicle(any(VehicleRequest.class));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when vehicle is updated successfully")
    public void testUpdateVehicle_ReturnsOk() {
        UserVehicle updatedVehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL01AB1234")
                .makerModel("Tata Nexon EV")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(vehicleService.updateVehicle(any(VehicleRequest.class))).thenReturn(updatedVehicle);

        VehicleRequest request = VehicleRequest.builder()
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .makerModel("Tata Nexon EV")
                .build();

        ResponseEntity<VehicleResponse> response = controller.updateVehicle(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getVehicles().size());
        assertEquals(TEST_VEHICLE_ID, response.getBody().getVehicles().get(0).getId());
        assertEquals("Tata Nexon EV", response.getBody().getVehicles().get(0).getMakerModel());
        verify(vehicleService, times(1)).updateVehicle(any(VehicleRequest.class));
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when searching vehicles with flat criteria")
    public void testSearchVehicles_FlatCriteria_ReturnsOk() {
        UserVehicle vehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL01AB1234")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(vehicleService.searchVehicles(any(VehicleSearchCriteria.class), any()))
                .thenReturn(Collections.singletonList(vehicle));

        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .tenantId("pg.citya")
                .build();

        ResponseEntity<VehicleResponse> response = controller.searchVehicles(criteria);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getVehicles().size());
        assertEquals(1, response.getBody().getTotalCount());
        verify(vehicleService, times(1)).searchVehicles(any(VehicleSearchCriteria.class), any());
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when searching vehicles with nested criteria wrapper")
    public void testSearchVehicles_NestedCriteria_ReturnsOk() {
        UserVehicle vehicle = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .vehicleNumber("DL01AB1234")
                .status(UserConstants.STATUS_ACTIVE_UPPERCASE)
                .build();

        when(vehicleService.searchVehicles(any(VehicleSearchCriteria.class), any()))
                .thenReturn(Collections.singletonList(vehicle));

        VehicleSearchCriteria nested = VehicleSearchCriteria.builder()
                .userUuid(TEST_USER_UUID)
                .build();

        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder()
                .requestInfo(RequestInfo.builder().build())
                .criteria(nested)
                .build();

        ResponseEntity<VehicleResponse> response = controller.searchVehicles(criteria);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getVehicles().size());
    }

    @Test
    @DisplayName("Should return HTTP 200 OK when deleting vehicle declaration")
    public void testDeleteVehicle_ReturnsOk() {
        UserVehicle deactivated = UserVehicle.builder()
                .id(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .status(UserConstants.STATUS_INACTIVE_UPPERCASE)
                .build();

        when(vehicleService.deleteVehicle(any(VehicleRequest.class))).thenReturn(deactivated);

        VehicleRequest request = VehicleRequest.builder()
                .vehicleId(TEST_VEHICLE_ID)
                .userUuid(TEST_USER_UUID)
                .build();

        ResponseEntity<VehicleResponse> response = controller.deleteVehicle(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getVehicles().size());
        assertEquals(UserConstants.STATUS_INACTIVE_UPPERCASE, response.getBody().getVehicles().get(0).getStatus());
        verify(vehicleService, times(1)).deleteVehicle(any(VehicleRequest.class));
    }
}
