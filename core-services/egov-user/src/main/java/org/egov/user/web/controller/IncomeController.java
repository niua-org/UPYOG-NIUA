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
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.domain.service.asset.IncomeService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.asset.IncomeRequest;
import org.egov.user.web.contract.asset.IncomeResponse;
import org.egov.user.web.contract.asset.IncomeSearchCriteria;
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
 * REST controller exposing standalone CRUD and lifecycle deactivation endpoints for citizen income declarations.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Exclusively manages HTTP transport and validation
 *       delegating domain logic to {@link IncomeService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Relies exclusively on injected domain service abstractions.</li>
 * </ul>
 *
 * <p>Available Endpoints (base path {@code /v1/income}):</p>
 * <ul>
 *   <li>{@code POST /_create} - Registers a new income declaration for a citizen</li>
 *   <li>{@code POST /_update} - Updates existing income declaration attributes or attached verification proofs</li>
 *   <li>{@code POST /_search} - Searches income declarations by citizen UUID, income ID, tenant, or fiscal identifier</li>
 *   <li>{@code POST /_delete} - Soft-deletes an income declaration by marking its status as {@code INACTIVE}</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "Income Management", description = "Standalone CRUD endpoints for citizen income declarations")
@RestController
@RequestMapping("/v1/income")
@Slf4j
public class IncomeController {

    private final IncomeService incomeService;

    /**
     * Constructs {@link IncomeController} with injected domain service.
     *
     * @param incomeService domain service orchestrating income operations
     */
    @Autowired
    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    /**
     * Creates a new citizen income declaration.
     *
     * @param request income creation request payload
     * @return response containing created income declaration
     */
    @Operation(
            summary = "Create Income Declaration",
            description = "Registers a new income and fiscal declaration for a citizen with occupation, annual income, identifier (PAN/TAN/GSTIN), and optional salary/tax proofs."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Income declaration created successfully",
                    content = @Content(schema = @Schema(implementation = IncomeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing mandatory fields"),
            @ApiResponse(responseCode = "404", description = "Citizen record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_create")
    public ResponseEntity<IncomeResponse> createIncome(@RequestBody @Valid IncomeRequest request) {
        log.info("[INCOME-CONTROLLER] Received income create request for citizen: {}",
                request != null ? request.resolveUserUuid() : null);

        UserIncome created = incomeService.createIncome(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IncomeResponse(responseInfo, created));
    }

    /**
     * Updates an existing citizen income declaration.
     *
     * @param request income update request payload
     * @return response containing updated income declaration
     */
    @Operation(
            summary = "Update Income Declaration",
            description = "Updates attributes, occupation, annual income, fiscal identifier, or attached proof documents of an existing income declaration."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Income declaration updated successfully",
                    content = @Content(schema = @Schema(implementation = IncomeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid income ID or request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized caller - income record belongs to another citizen"),
            @ApiResponse(responseCode = "404", description = "Income record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_update")
    public ResponseEntity<IncomeResponse> updateIncome(@RequestBody @Valid IncomeRequest request) {
        log.info("[INCOME-CONTROLLER] Received income update request for income ID: {}",
                request != null ? request.resolveIncomeId() : null);

        UserIncome updated = incomeService.updateIncome(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.ok(new IncomeResponse(responseInfo, updated));
    }

    /**
     * Searches citizen income declarations matching criteria filters.
     *
     * @param criteria income search criteria payload
     * @return response containing list of matched income declarations
     */
    @Operation(
            summary = "Search Income Declarations",
            description = "Queries citizen income declarations matching criteria filters (userUuid, incomeId, tenantId, occupation, identifierType, identifierValue). Defaults to active declarations."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Income declarations retrieved successfully",
                    content = @Content(schema = @Schema(implementation = IncomeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_search")
    public ResponseEntity<IncomeResponse> searchIncome(@RequestBody @Valid IncomeSearchCriteria criteria) {
        if (criteria != null && criteria.getCriteria() != null) {
            IncomeSearchCriteria inner = criteria.getCriteria();
            if (inner.getRequestInfo() == null) {
                inner.setRequestInfo(criteria.getRequestInfo());
            }
            criteria = inner;
        }

        log.info("[INCOME-CONTROLLER] Received income search request: userUuid={}, incomeId={}, tenantId={}",
                criteria != null ? criteria.getUserUuid() : null,
                criteria != null ? criteria.resolveIncomeId() : null,
                criteria != null ? criteria.getTenantId() : null);

        List<UserIncome> incomes = incomeService.searchIncome(
                criteria, criteria != null ? criteria.getRequestInfo() : null);

        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        IncomeResponse response = IncomeResponse.builder()
                .responseInfo(responseInfo)
                .incomes(incomes != null ? incomes : Collections.emptyList())
                .totalCount(incomes != null ? incomes.size() : 0)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deactivates an income declaration by marking its status as {@code INACTIVE}.
     *
     * @param request income delete request payload
     * @return response containing deactivated income declaration
     */
    @Operation(
            summary = "Delete Income Declaration",
            description = "Soft-deletes an income declaration by setting its status to INACTIVE. Used during member information updates when a citizen removes or re-declares income."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Income declaration marked as inactive successfully",
                    content = @Content(schema = @Schema(implementation = IncomeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid income ID or user UUID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized caller - income record belongs to another citizen"),
            @ApiResponse(responseCode = "404", description = "Income record not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_delete")
    public ResponseEntity<IncomeResponse> deleteIncome(@RequestBody @Valid IncomeRequest request) {
        log.info("[INCOME-CONTROLLER] Received income deactivation request for income ID: {}, citizen: {}",
                request != null ? request.resolveIncomeId() : null,
                request != null ? request.resolveUserUuid() : null);

        UserIncome deactivated = incomeService.deleteIncome(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build();
        return ResponseEntity.ok(new IncomeResponse(responseInfo, deactivated));
    }
}
