package org.egov.user.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.AddressSearchCriteria;
import org.egov.user.domain.service.address.AddressMasterDataService;
import org.egov.user.domain.service.address.AddressService;
import org.egov.user.web.contract.AddressCacheRefreshRequest;
import org.egov.user.web.contract.AddressCacheRefreshResponse;
import org.egov.user.web.contract.AddressRequest;
import org.egov.user.web.contract.AddressResponse;
import org.egov.user.web.contract.AddressSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller exposing citizen address CRUD operations under {@code /address}.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport and serialization,
 *       delegating business logic and soft-versioning to {@link AddressService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Depends purely on injected service abstractions.</li>
 * </ul>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /address/_create} - Create address with door no, street, locality, pincode</li>
 *   <li>{@code POST /address/_update} - Update address with automated soft-versioning</li>
 *   <li>{@code POST /address/_search} - Search addresses by criteria</li>
 *   <li>{@code POST /address/_delete} - Soft-delete / deactivate address</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "Address V2", description = "Citizen address management endpoints with SCD Type 2 history soft-versioning")
@RestController
@RequestMapping("/address")
@Slf4j
public class AddressController {

    private final AddressService addressService;
    private final AddressMasterDataService addressMasterDataService;

    @Autowired
    public AddressController(AddressService addressService,
                             @Autowired(required = false) AddressMasterDataService addressMasterDataService) {
        this.addressService = addressService;
        this.addressMasterDataService = addressMasterDataService;
    }

    public AddressController(AddressService addressService) {
        this(addressService, null);
    }

    /**
     * Creates a new citizen address.
     *
     * @param request address create request payload
     * @return response containing created address
     */
    @Operation(
            summary = "Create Address",
            description = "Creates a new address record for a citizen with door number, street, landmark, city, and pincode. Defaults status to ACTIVE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Address created successfully",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing mandatory fields"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_create")
    public ResponseEntity<AddressResponse> createAddress(@RequestBody @Valid AddressRequest request) {
        log.info("[ADDRESS-CONTROLLER] Received address create request for uuid: {}",
                request != null ? request.getUuid() : null);

        Address created = addressService.createAddress(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(HttpStatus.CREATED.toString()).build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AddressResponse(responseInfo, Collections.singletonList(created)));
    }

    /**
     * Updates an address with soft-versioning.
     *
     * @param request address update request payload
     * @return response containing updated address
     */
    @Operation(
            summary = "Update Address",
            description = "Updates an existing address using SCD Type 2 soft-versioning: marks the old record as INACTIVE and inserts a new ACTIVE version with an incremented version number."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address updated successfully",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid address ID or request payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_update")
    public ResponseEntity<AddressResponse> updateAddress(@RequestBody @Valid AddressRequest request) {
        log.info("[ADDRESS-CONTROLLER] Received address update request for addressId: {}",
                request != null ? request.getAddressId() : null);

        Address updated = addressService.updateAddress(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return ResponseEntity.ok(new AddressResponse(responseInfo, Collections.singletonList(updated)));
    }

    /**
     * Searches addresses based on criteria.
     *
     * @param request address search request payload
     * @return response containing list of matched addresses
     */
    @Operation(
            summary = "Search Addresses",
            description = "Searches citizen addresses by user UUID, tenant ID, address type (PERMANENT/CORRESPONDENCE), or city."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_search")
    public ResponseEntity<AddressResponse> searchAddress(@RequestBody @Valid AddressSearchRequest request) {
        AddressSearchCriteria criteria;
        if (request != null && request.getCriteria() != null) {
            criteria = request.getCriteria();
            if (StringUtils.isBlank(criteria.getUserUuid())) {
                criteria.setUserUuid(request.getUuid());
            }
            if (StringUtils.isBlank(criteria.getTenantId())) {
                criteria.setTenantId(request.getTenantId());
            }
        } else if (request != null) {
            criteria = AddressSearchCriteria.builder()
                    .userUuid(request.getUuid())
                    .userId(request.getUserId())
                    .tenantId(request.getTenantId())
                    .addressType(request.getAddressType())
                    .city(request.getCity())
                    .pinCode(request.getPinCode())
                    .id(request.getId())
                    .status(request.getStatus())
                    .build();
        } else {
            criteria = AddressSearchCriteria.builder().build();
        }

        log.info("[ADDRESS-CONTROLLER] Received address search request for uuid: {}, tenantId: {}",
                criteria.getUserUuid(), criteria.getTenantId());

        List<Address> addresses = addressService.searchAddress(criteria, request != null ? request.getRequestInfo() : null);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return ResponseEntity.ok(new AddressResponse(responseInfo, addresses));
    }

    /**
     * Soft-deletes / deactivates an address.
     *
     * @param request address delete request payload
     * @return response containing deactivated address
     */
    @Operation(
            summary = "Delete Address",
            description = "Soft-deletes / deactivates an address by setting its status to INACTIVE."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address deactivated successfully",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid address ID or request payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_delete")
    public ResponseEntity<AddressResponse> deleteAddress(@RequestBody @Valid AddressRequest request) {
        log.info("[ADDRESS-CONTROLLER] Received address delete request for addressId: {}",
                request != null ? request.getAddressId() : null);

        Address deleted = addressService.deleteAddress(request);
        ResponseInfo responseInfo = ResponseInfo.builder().status(String.valueOf(HttpStatus.OK.value())).build();
        return ResponseEntity.ok(new AddressResponse(responseInfo, Collections.singletonList(deleted)));
    }

    /**
     * Manually refreshes or evicts the Address Type cache from MDMS or application properties.
     *
     * @param request cache refresh request payload (optional tenantId and RequestInfo)
     * @return response indicating cache refresh status and latest allowed address types
     */
    @Operation(
            summary = "Refresh Address Types Master Data Cache",
            description = "Flushes and updates the cached address types from MDMS/application.properties. If tenantId is provided, refreshes that tenant; otherwise evicts the cache globally."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address type cache refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AddressCacheRefreshResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during cache refresh")
    })
    @PostMapping(value = {"/_refreshCache", "/v1/_refreshCache", "/_clearCache", "/v1/_clearCache"})
    public ResponseEntity<AddressCacheRefreshResponse> refreshAddressTypeCache(
            @RequestBody(required = false) AddressCacheRefreshRequest request) {

        RequestInfo requestInfo = request != null ? request.getRequestInfo() : null;
        String tenantId = request != null ? request.getTenantId() : null;

        log.info("[ADDRESS-CONTROLLER] Received manual cache refresh request for tenant: {}", tenantId);

        Set<String> refreshedTypes = Collections.emptySet();
        Map<String, Set<String>> allCachedTypes = Collections.emptyMap();
        String message;

        if (addressMasterDataService != null) {
            if (StringUtils.isNotBlank(tenantId)) {
                refreshedTypes = addressMasterDataService.refreshCache(requestInfo, tenantId);
                message = "Address type cache refreshed successfully for tenant: " + tenantId;
            } else {
                addressMasterDataService.clearCache();
                message = "Address type cache cleared globally across all tenants";
            }
            allCachedTypes = addressMasterDataService.getCachedAddressTypes();
        } else {
            message = "AddressMasterDataService is not active in current context";
        }

        ResponseInfo responseInfo = ResponseInfo.builder()
                .status(String.valueOf(HttpStatus.OK.value()))
                .build();

        AddressCacheRefreshResponse response = AddressCacheRefreshResponse.builder()
                .responseInfo(responseInfo)
                .tenantId(tenantId)
                .allowedAddressTypes(refreshedTypes)
                .cachedAddressTypesByTenant(allCachedTypes)
                .message(message)
                .build();

        return ResponseEntity.ok(response);
    }
}
