package org.egov.user.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.asset.UserVehicle;
import org.egov.user.domain.service.asset.VehicleService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.VehicleRequest;
import org.egov.user.web.contract.asset.VehicleResponse;
import org.egov.user.web.contract.asset.VehicleSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST controller exposing standalone CRUD and lifecycle deactivation endpoints for citizen vehicle declarations.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Exclusively manages HTTP transport and validation
 *       delegating domain logic to {@link VehicleService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Relies exclusively on injected domain service abstractions.</li>
 * </ul>
 *
 * <p>Available Endpoints (base path {@code /vehicle}):</p>
 * <ul>
 *   <li>{@code POST /_create} - Registers a new vehicle declaration for a citizen</li>
 *   <li>{@code POST /_update} - Updates existing vehicle attributes or attached RC documents</li>
 *   <li>{@code POST /_search} - Searches vehicles by citizen UUID, vehicle ID, registration number, or tenant</li>
 *   <li>{@code POST /_delete} - Soft-deletes a vehicle declaration by marking its status as {@code INACTIVE}</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "Vehicle Management", description = "Standalone CRUD endpoints for citizen vehicle declarations")
@RestController
@RequestMapping("/vehicle")
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Constructs {@link VehicleController} with injected domain service.
     *
     * @param vehicleService domain service orchestrating vehicle operations
     */
    @Autowired
    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Creates a new vehicle declaration for a citizen.
     *
     * @param request vehicle creation request payload
     * @return response containing created vehicle
     */
    @Operation(
            summary = "Create Vehicle Declaration",
            description = "Registers a new motor vehicle declaration for a citizen with vehicle registration plate number, classification type, fuel type, and optional RC document."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehicle declaration created successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing mandatory fields"),
            @ApiResponse(responseCode = "404", description = "Citizen record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_create")
    public ResponseEntity<VehicleResponse> createVehicle(@RequestBody @Valid VehicleRequest request) {
        log.info("[VEHICLE-CONTROLLER] Received vehicle create request for citizen: {}",
                request != null ? request.resolveUserUuid() : null);

        UserVehicle created = vehicleService.createVehicle(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new VehicleResponse(responseInfo, created));
    }

    /**
     * Updates an existing vehicle declaration for a citizen.
     *
     * @param request vehicle update request payload
     * @return response containing updated vehicle
     */
    @Operation(
            summary = "Update Vehicle Declaration",
            description = "Updates attributes, maker/model, fuel type, category, or attached RC document of an existing citizen vehicle declaration."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle declaration updated successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid vehicle ID or request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized caller - vehicle belongs to another citizen"),
            @ApiResponse(responseCode = "404", description = "Vehicle record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_update")
    public ResponseEntity<VehicleResponse> updateVehicle(@RequestBody @Valid VehicleRequest request) {
        log.info("[VEHICLE-CONTROLLER] Received vehicle update request for vehicle ID: {}",
                request != null ? request.resolveVehicleId() : null);

        UserVehicle updated = vehicleService.updateVehicle(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.ok(new VehicleResponse(responseInfo, updated));
    }

    /**
     * Searches citizen vehicle declarations matching criteria filters.
     *
     * @param criteria vehicle search criteria payload
     * @return response containing list of matched vehicles
     */
    @Operation(
            summary = "Search Vehicle Declarations",
            description = "Queries citizen vehicle declarations matching criteria filters (userUuid, vehicleId, vehicleNumber, tenantId, vehicleType, fuelType, category). Defaults to active vehicles."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_search")
    public ResponseEntity<VehicleResponse> searchVehicles(@RequestBody @Valid VehicleSearchCriteria criteria) {
        if (criteria != null && criteria.getCriteria() != null) {
            VehicleSearchCriteria inner = criteria.getCriteria();
            if (inner.getRequestInfo() == null) {
                inner.setRequestInfo(criteria.getRequestInfo());
            }
            criteria = inner;
        }

        log.info("[VEHICLE-CONTROLLER] Received vehicle search request: userUuid={}, vehicleId={}, tenantId={}",
                criteria != null ? criteria.getUserUuid() : null,
                criteria != null ? criteria.getVehicleId() : null,
                criteria != null ? criteria.getTenantId() : null);

        List<UserVehicle> vehicles = vehicleService.searchVehicles(
                criteria, criteria != null ? criteria.getRequestInfo() : null);

        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        VehicleResponse response = VehicleResponse.builder()
                .responseInfo(responseInfo)
                .vehicles(vehicles != null ? vehicles : Collections.emptyList())
                .totalCount(vehicles != null ? vehicles.size() : 0)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deactivates a vehicle declaration by marking its status as {@code INACTIVE}.
     *
     * @param request vehicle delete request payload
     * @return response containing deactivated vehicle
     */
    @Operation(
            summary = "Delete Vehicle Declaration",
            description = "Soft-deletes a vehicle declaration by setting its status to INACTIVE. Used during member information updates when a citizen sells or disposes of a vehicle."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle marked as inactive successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid vehicle ID or user UUID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized caller - vehicle belongs to another citizen"),
            @ApiResponse(responseCode = "404", description = "Vehicle record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_delete")
    public ResponseEntity<VehicleResponse> deleteVehicle(@RequestBody @Valid VehicleRequest request) {
        log.info("[VEHICLE-CONTROLLER] Received vehicle deactivation request for vehicle ID: {}, citizen: {}",
                request != null ? request.resolveVehicleId() : null,
                request != null ? request.resolveUserUuid() : null);

        UserVehicle deactivated = vehicleService.deleteVehicle(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.ok(new VehicleResponse(responseInfo, deactivated));
    }
}
