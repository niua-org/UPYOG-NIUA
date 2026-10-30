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
import org.egov.user.domain.model.User;
import org.egov.user.domain.service.profile.ProfileService;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.UserSearchResponseContent;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.egov.user.web.contract.profile.ProfileIncludeEntity;
import org.egov.user.web.contract.profile.ProfileInitRequest;
import org.egov.user.web.contract.profile.ProfileResyncRequest;
import org.egov.user.web.contract.profile.ProfileResponse;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller exposing citizen profile lifecycle endpoints under {@code /profile}.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles HTTP transport, serialization,
 *       and delegates business logic and validations to {@link ProfileService}.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Depends purely on injected abstractions.</li>
 * </ul>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /profile/_create} - Full citizen profile creation with Section 8 mandatory field enforcement</li>
 *   <li>{@code POST /profile/_init} - Lightweight profile initialization</li>
 *   <li>{@code POST /profile/_update} - Profile demographic updates with Section 8 non-editable field locking</li>
 *   <li>{@code POST /profile/_search} - Profile search with decrypted PII</li>
 *   <li>{@code POST /profile/_resync} - Explicit Aadhaar re-sync trigger for UI</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Tag(name = "Citizen Profile", description = "Citizen profile lifecycle management endpoints including creation, initialization, updates with source-based locking, search, and Aadhaar re-sync")
@RestController
@RequestMapping("/profile")
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Constructs the profile controller with the citizen profile orchestrator service.
     *
     * @param profileService citizen profile domain service
     */
    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Creates a complete citizen profile enforcing all Section 8 mandatory fields
     * (demographics, address, and document proofs).
     *
     * @param request profile create request payload
     * @return response containing created citizen profile
     */
    @Operation(
            summary = "Create Citizen Profile",
            description = "Creates a complete citizen profile enforcing all Section 8 mandatory fields (Name, Father's Name, Mother's Name, Mobile Number, DOB, Email ID, Gender, Address details, and Document Proofs)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile created successfully",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing mandatory fields or validation failure"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_create")
    public ResponseEntity<ProfileResponse> createProfile(@RequestBody @Valid ProfileCreateRequest request) {
        if (request != null) {
            request.unpackUserIfPresent();
        }
        log.info("[PROFILE-CONTROLLER] Received profile create request for mobile: {}, tenant: {}",
                maskMobile(request != null ? request.getMobileNumber() : null),
                request != null ? request.getTenantId() : null);

        User user = profileService.createProfile(request);
        return buildSingleUserResponse(user);
    }

    /**
     * Initializes a lightweight citizen profile.
     *
     * @param request profile init request payload
     * @return response containing initialized profile
     */
    @Operation(
            summary = "Initialize Profile",
            description = "Initializes a lightweight citizen profile upon first registration or mobile OTP login. Automatically defaults verification source to USER."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile initialized successfully",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failure"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_init")
    public ResponseEntity<ProfileResponse> initProfile(@RequestBody @Valid ProfileInitRequest request) {
        if (request != null) {
            request.unpackUserIfPresent();
        }
        log.info("[PROFILE-CONTROLLER] Received profile init request for mobile: {}, tenant: {}",
                maskMobile(request != null ? request.getMobileNumber() : null),
                request != null ? request.getTenantId() : null);

        User user = profileService.initProfile(request);
        return buildSingleUserResponse(user);
    }

    /**
     * Updates citizen profile details, enforcing source-based restrictions
     * (locking Name, DOB, Gender if verified unless re-sync flag is provided).
     *
     * @param request profile update request payload
     * @return response containing updated profile
     */
    @Operation(
            summary = "Update Profile",
            description = "Updates citizen demographic profile details with source-based attribute locking (UIDAI vs USER). If fields were verified via UIDAI, edits are blocked unless re-sync is enabled."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or attempting to modify locked verified fields"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_update")
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        log.info("[PROFILE-CONTROLLER] Received profile update request. isResync: {}",
                request != null ? request.getIsResync() : false);

        User user = profileService.updateProfile(request);
        return buildSingleUserResponse(user);
    }

    /**
     * Searches citizen profiles based on criteria.
     * <p>
     * Supports modular entity inclusion via the {@code include} parameter:
     * <ul>
     *   <li>Allowed values: {@code PROFILE}, {@code ADDRESS}, {@code DOCUMENT}</li>
     *   <li>Default value: {@code [PROFILE]} if {@code include} is omitted or empty</li>
     *   <li>{@code ADDRESS}: Enriches response with active postal addresses of matched citizens</li>
     *   <li>{@code DOCUMENT}: Enriches response with active uploaded KYC and identity proof documents</li>
     * </ul>
     *
     * @param request profile search request payload containing filter criteria and optional include sections
     * @return response containing list of matched decrypted profiles
     */
    @Operation(
            summary = "Search Profile",
            description = "Searches citizen profiles by UUID, tenant ID, or mobile number, with automated PII decryption for authorized consumers. " +
                    "The optional 'include' parameter controls which entity sections are returned (allowed values: PROFILE, ADDRESS, DOCUMENT; default: [PROFILE]). " +
                    "Specifying ADDRESS enriches the response with citizen address details, while DOCUMENT enriches with uploaded KYC document proofs."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profiles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria or invalid include entity"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_search")
    public ResponseEntity<ProfileResponse> searchProfile(@RequestBody @Valid ProfileSearchRequest request) {
        log.info("[PROFILE-CONTROLLER] Received profile search request. tenant: {}, mobile: {}, include: {}",
                request != null ? request.getTenantId() : null,
                maskMobile(request != null ? request.getMobileNumber() : null),
                request != null ? request.getInclude() : null);

        Set<ProfileIncludeEntity> normalizedIncludes = profileService.resolveIncludes(request != null ? request.getInclude() : null);
        List<User> users = profileService.searchProfile(request, normalizedIncludes);

        List<UserSearchResponseContent> contents = users != null
                ? users.stream().map(u -> new UserSearchResponseContent(u, normalizedIncludes)).collect(Collectors.toList())
                : Collections.emptyList();

        ProfileResponse response = ProfileResponse.builder()
                .responseInfo(ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build())
                .user(contents)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }




    /**
     * Dedicated endpoint to re-synchronize citizen profile with Aadhaar e-KYC.
     * Triggered directly by the UI "Re-sync with Aadhaar" button.
     *
     * @param request profile re-sync request payload
     * @return response containing refreshed profile with updated last_resync_date
     */
    @Operation(
            summary = "Re-sync Profile with Aadhaar",
            description = "Dedicated endpoint triggered by the citizen UI to re-synchronize profile demographics directly with UIDAI e-KYC data, resetting the re-sync timer."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile re-synchronized successfully with UIDAI",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid citizen UUID or request payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/_resync")
    public ResponseEntity<ProfileResponse> resyncProfile(@RequestBody @Valid ProfileResyncRequest request) {
        if (request != null) {
            request.unpackUserIfPresent();
        }
        log.info("[PROFILE-CONTROLLER] Received explicit profile re-sync request for citizen UUID: {}",
                request != null ? request.getUuid() : null);

        User user = profileService.resyncProfile(request);
        return buildSingleUserResponse(user);
    }

    private ResponseEntity<ProfileResponse> buildSingleUserResponse(User user) {
        List<UserSearchResponseContent> contents = user != null
                ? Collections.singletonList(new UserSearchResponseContent(user))
                : Collections.emptyList();

        ProfileResponse response = ProfileResponse.builder()
                .responseInfo(ResponseInfo.builder().status(UserConstants.STATUS_SUCCESS).build())
                .user(contents)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
    }
}
