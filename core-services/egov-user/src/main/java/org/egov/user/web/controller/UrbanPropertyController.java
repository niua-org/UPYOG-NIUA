package org.egov.user.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.egov.user.domain.service.asset.UrbanPropertyService;
import org.egov.user.util.MaskUtils;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateRequest;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing urban property ownership validation, OTP dispatch,
 * OTP verification, and consent validation endpoints for property linking workflows.
 */
@Tag(name = "Urban Property Management", description = "Endpoints for validating ownership, sending OTPs, and verifying authorization consent")
@RestController
@RequestMapping("/v1/urban-property")
@Slf4j
public class UrbanPropertyController {

    private final UrbanPropertyService urbanPropertyService;

    /**
     * Constructs {@link UrbanPropertyController} with injected domain service.
     *
     * @param urbanPropertyService domain service orchestrating urban property operations
     */
    @Autowired
    public UrbanPropertyController(UrbanPropertyService urbanPropertyService) {
        this.urbanPropertyService = urbanPropertyService;
    }

    /**
     * Enforces individual and multiple-owner property linking rules, then dispatches OTP to the property owner's mobile.
     *
     * @param request OTP dispatch request containing propertyId, tenantId, and optional target owner mobile
     * @return response entity containing referenceId and masked mobile number
     */
    @Operation(summary = "Send Property OTP", description = "Validates ownership type and linkage capacity, then dispatches OTP to the property owner's registered mobile number.")
    @PostMapping("/otp/_send")
    public ResponseEntity<UrbanPropertyOtpResponse> sendUrbanPropertyOtp(@Valid @RequestBody UrbanPropertyOtpRequest request) {
        log.info("[URBAN-PROPERTY-CONTROLLER] Received /otp/_send request for property: {}, tenant: {}",
                request.getPropertyId(), request.getTenantId());
        UrbanPropertyOtpResponse response = urbanPropertyService.sendUrbanPropertyOtp(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Verifies the submitted OTP and generates an authorization consent token.
     *
     * @param request OTP verification request envelope containing referenceId, propertyId, and OTP
     * @return response entity containing verified property consent token and expiry
     */
    @Operation(summary = "Verify Property OTP", description = "Verifies submitted OTP and issues a propertyConsentToken bound to the propertyId.")
    @PostMapping("/otp/_verify")
    public ResponseEntity<UrbanPropertyOtpVerifyResponse> verifyUrbanPropertyOtp(@Valid @RequestBody UrbanPropertyOtpVerifyRequest request) {
        log.info("[URBAN-PROPERTY-CONTROLLER] Received /otp/_verify request for reference: {}, property: {}",
                MaskUtils.maskIdentifier(request.getReferenceId()), request.getPropertyId());
        UrbanPropertyOtpVerifyResponse response = urbanPropertyService.verifyUrbanPropertyOtp(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Validates whether a propertyConsentToken is valid and unexpired for a given propertyId.
     * <p><b>NOTE:</b> This endpoint is strictly an internal inter-service RPC designed exclusively
     * for verification by trusted internal microservices (such as {@code upyog-family-service}).
     * It must NEVER be exposed publicly or registered in MDMS {@code ACCESSCONTROL-ACTIONS-TEST.json} /
     * {@code roleactions.json} with external or citizen-facing roles.</p>
     *
     * @param request consent token validation request envelope
     * @return response entity indicating token validity
     */
    @Operation(summary = "Validate Consent Token (Internal RPC)", description = "Verifies that a propertyConsentToken is valid, active, and matches the target propertyId. Internal RPC only.")
    @PostMapping("/consent/_validate")
    public ResponseEntity<UrbanPropertyConsentValidateResponse> validateConsentToken(@Valid @RequestBody UrbanPropertyConsentValidateRequest request) {
        log.info("[URBAN-PROPERTY-CONTROLLER] Received /consent/_validate request for property: {}", request.getPropertyId());
        UrbanPropertyConsentValidateResponse response = urbanPropertyService.validateConsentToken(request);
        return ResponseEntity.ok(response);
    }
}
