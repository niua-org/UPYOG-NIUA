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
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.domain.service.asset.RuralPropertyService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.RuralPropertyRequest;
import org.egov.user.web.contract.asset.RuralPropertyResponse;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;
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
 * REST controller exposing standalone CRUD and lifecycle deactivation endpoints for citizen rural properties.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Exclusively manages HTTP transport and validation
 *       delegating domain logic to {@link RuralPropertyService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Relies exclusively on injected domain service abstractions.</li>
 * </ul>
 *
 * <p>Available Endpoints (available under both {@code /user/property/rural} and {@code /user/rural-property}):</p>
 * <ul>
 *   <li>{@code POST /_create} - Creates a new rural property declaration for a citizen</li>
 *   <li>{@code POST /_update} - Updates existing rural property attributes, co-owners, or documents</li>
 *   <li>{@code POST /_search} - Searches rural properties by citizen UUID, property ID, tenant, or survey number</li>
 *   <li>{@code POST /_delete} - Soft-deletes a rural property by marking its status as {@code INACTIVE}</li>
 *   <li>{@code POST /_deactivate} - Alias endpoint marking a rural property as {@code INACTIVE}</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "Rural Property Management", description = "Standalone CRUD and lifecycle deactivation endpoints for citizen rural properties")
@RestController
@RequestMapping({"/property/rural", "/rural-property"})
@Slf4j
public class RuralPropertyController {

    private final RuralPropertyService ruralPropertyService;

    /**
     * Constructs {@link RuralPropertyController} with injected domain service.
     *
     * @param ruralPropertyService domain service orchestrating rural property operations
     */
    @Autowired
    public RuralPropertyController(RuralPropertyService ruralPropertyService) {
        this.ruralPropertyService = ruralPropertyService;
    }

    /**
     * Creates a new rural property declaration for a citizen.
     *
     * @param request rural property creation request payload
     * @return response containing created rural property
     */
    @Operation(
            summary = "Create Rural Property",
            description = "Registers a new rural landholding or property declaration for a citizen with Gram Panchayat, village name, survey number, and optional co-owners."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rural property created successfully",
                    content = @Content(schema = @Schema(implementation = RuralPropertyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing mandatory fields"),
            @ApiResponse(responseCode = "404", description = "Citizen record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_create")
    public ResponseEntity<RuralPropertyResponse> createRuralProperty(@RequestBody @Valid RuralPropertyRequest request) {
        log.info("[RURAL-PROP-CONTROLLER] Received rural property create request for citizen: {}",
                request != null ? request.resolveUserUuid() : null);

        UserProperty created = ruralPropertyService.createRuralProperty(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RuralPropertyResponse(responseInfo, created));
    }

    /**
     * Updates an existing rural property declaration for a citizen.
     *
     * @param request rural property update request payload
     * @return response containing updated rural property
     */
    @Operation(
            summary = "Update Rural Property",
            description = "Updates attributes, Gram Panchayat, village name, survey number, co-owners, or documents of an existing rural property declaration."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rural property updated successfully",
                    content = @Content(schema = @Schema(implementation = RuralPropertyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid property ID or request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized caller - property belongs to another citizen"),
            @ApiResponse(responseCode = "404", description = "Property record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_update")
    public ResponseEntity<RuralPropertyResponse> updateRuralProperty(@RequestBody @Valid RuralPropertyRequest request) {
        log.info("[RURAL-PROP-CONTROLLER] Received rural property update request for property ID: {}",
                request != null ? request.resolvePropertyId() : null);

        UserProperty updated = ruralPropertyService.updateRuralProperty(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.ok(new RuralPropertyResponse(responseInfo, updated));
    }

    /**
     * Searches citizen rural properties by user UUID, property ID, tenant ID, or survey number.
     *
     * @param criteria rural property search criteria payload
     * @return response containing list of matched rural properties
     */
    @Operation(
            summary = "Search Rural Properties",
            description = "Queries rural properties matching criteria filters (userUuid, propertyId, tenantId, gramPanchayat, villageName, surveyNumber). Defaults to active properties."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rural properties retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RuralPropertyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_search")
    public ResponseEntity<RuralPropertyResponse> searchRuralProperties(@RequestBody @Valid RuralPropertySearchCriteria criteria) {
        if (criteria != null && criteria.getCriteria() != null) {
            RuralPropertySearchCriteria inner = criteria.getCriteria();
            if (inner.getRequestInfo() == null) {
                inner.setRequestInfo(criteria.getRequestInfo());
            }
            criteria = inner;
        }

        log.info("[RURAL-PROP-CONTROLLER] Received rural property search request: userUuid={}, propertyId={}, tenantId={}",
                criteria != null ? criteria.getUserUuid() : null,
                criteria != null ? criteria.getPropertyId() : null,
                criteria != null ? criteria.getTenantId() : null);

        List<UserProperty> properties = ruralPropertyService.searchRuralProperties(
                criteria, criteria != null ? criteria.getRequestInfo() : null);

        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        RuralPropertyResponse response = RuralPropertyResponse.builder()
                .responseInfo(responseInfo)
                .properties(properties != null ? properties : Collections.emptyList())
                .totalCount(properties != null ? properties.size() : 0)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deactivates a rural property declaration by marking its status as {@code INACTIVE}.
     *
     * @param request rural property delete request payload
     * @return response containing deactivated property
     */
    @Operation(
            summary = "Delete / Deactivate Rural Property",
            description = "Soft-deletes a rural property declaration by setting its status to INACTIVE. Used during member information updates when a citizen removes or sells a landholding."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rural property marked as inactive successfully",
                    content = @Content(schema = @Schema(implementation = RuralPropertyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid property ID or user UUID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized caller - property belongs to another citizen"),
            @ApiResponse(responseCode = "404", description = "Property record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = {"/_delete", "/_deactivate"})
    public ResponseEntity<RuralPropertyResponse> deleteRuralProperty(@RequestBody @Valid RuralPropertyRequest request) {
        log.info("[RURAL-PROP-CONTROLLER] Received rural property deactivation request for property ID: {}, citizen: {}",
                request != null ? request.resolvePropertyId() : null,
                request != null ? request.resolveUserUuid() : null);

        UserProperty deactivated = ruralPropertyService.deleteRuralProperty(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.ok(new RuralPropertyResponse(responseInfo, deactivated));
    }
}
