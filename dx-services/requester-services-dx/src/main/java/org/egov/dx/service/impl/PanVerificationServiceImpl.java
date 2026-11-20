package org.egov.dx.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorPanVerificationResponse;
import org.egov.dx.adapter.vendor.pan.PanGatewayAdapter;
import org.egov.dx.service.PanVerificationService;
import org.egov.dx.service.TenantVerificationConfigService;
import org.egov.dx.util.PanConstants;
import org.egov.dx.util.PanNumberUtils;
import org.egov.dx.validator.PanVerificationValidator;
import org.egov.dx.web.models.PanDetails;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.dx.web.models.PanVerificationResponse;
import org.egov.dx.web.models.ResponseInfoFactory;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of {@link PanVerificationService} orchestrating validation, vendor transformation,
 * gateway execution, and domain response enrichment for PAN verification.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles PAN verification domain business logic, separating it from the HTTP transport controller.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects polymorphic {@link PanGatewayAdapter} abstraction.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Vendor schema changes are isolated to this service layer without polluting REST controllers.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class PanVerificationServiceImpl implements PanVerificationService {

    private final PanGatewayAdapter gatewayAdapter;
    private final PanVerificationValidator validator;
    private final TenantVerificationConfigService tenantConfigService;

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param gatewayAdapter abstraction for active vendor/mock PAN gateway adapter
     * @param validator component handling request validation
     * @param tenantConfigService component checking tenant-level MDMS enablement
     */
    @Autowired
    public PanVerificationServiceImpl(PanGatewayAdapter gatewayAdapter,
                                      PanVerificationValidator validator,
                                      TenantVerificationConfigService tenantConfigService) {
        this.gatewayAdapter = gatewayAdapter;
        this.validator = validator;
        this.tenantConfigService = tenantConfigService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates request, enforces MDMS tenant configuration, delegates to PAN adapter,
     * sanitizes PII, and returns verification response.</p>
     */
    @Override
    public PanVerificationResponse verifyPan(PanVerificationRequest request) {
        // 1. Validate incoming request (SRP)
        validator.validatePanRequest(request);

        // Multi-tenant authorization guard (Review comment Medium #3): Verify that the tenant/ULB
        // has enabled the PAN verification module in MDMS configuration before invoking external APIs
        if (!tenantConfigService.isVerificationEnabled(request.getRequestInfo(), request.getTenantId(), "PAN")) {
            log.warn("PAN verification is not enabled in MDMS for tenant: {}", request.getTenantId());
            throw new CustomException(PanConstants.ERR_CODE_TENANT_DISABLED,
                    "PAN verification is not enabled for tenant: " + request.getTenantId() + " in MDMS configuration.");
        }

        String rawPan = request.getPan();
        String normalizedPan = PanNumberUtils.normalizePan(rawPan);
        String maskedPan = PanNumberUtils.maskPan(normalizedPan);

        log.debug("Processing PAN verification in service for masked PAN: {}, apiId: {}",
                maskedPan, request.getRequestInfo() != null ? request.getRequestInfo().getApiId() : "null");
        log.info("Processing PAN verification in service for masked PAN: {}, tenantId: {}",
                maskedPan, request.getTenantId());

        String requestId = (request.getRequestInfo() != null && StringUtils.isNotBlank(request.getRequestInfo().getMsgId()))
                ? request.getRequestInfo().getMsgId()
                : UUID.randomUUID().toString();

        // 2. Transform domain request to vendor DTO
        VendorPanVerificationRequest vendorReq = VendorPanVerificationRequest.builder()
                .header(VendorPanVerificationRequest.RequestHeader.builder()
                        .requestId(requestId)
                        .serviceId(PanConstants.DEFAULT_SERVICE_ID)
                        .timestamp(Instant.now().toString())
                        .version(PanConstants.DEFAULT_VERSION)
                        .build())
                .searchParams(VendorPanVerificationRequest.SearchParams.builder()
                        .pan(normalizedPan)
                        .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                        .dob(request.getDob() != null ? request.getDob().trim() : null)
                        .gender(request.getGender() != null ? request.getGender().trim() : null)
                        .build())
                .build();

        // 3. Delegate to gateway adapter abstraction (DIP)
        VendorPanVerificationResponse vendorRes = gatewayAdapter.verifyPan(vendorReq);

        // Resilience guard: handle null return from adapter (e.g., unexpected network failure or unhandled exception)
        if (vendorRes == null) {
            log.error("PAN Gateway returned null response for masked PAN: {}", maskedPan);
            throw new CustomException(PanConstants.ERR_CODE_GATEWAY_ERROR, PanConstants.MSG_GATEWAY_ERROR);
        }

        boolean isMock = gatewayAdapter.getClass().getSimpleName().startsWith("Mock");
        String source = isMock ? PanConstants.SOURCE_MOCK : PanConstants.SOURCE_LIVE_ITD;

        // 4. Handle vendor-level error payload (e.g. record not found or invalid credentials)
        if (vendorRes.getErrorDetails() != null) {
            log.warn("ITD PAN gateway returned error for masked PAN {}: {} - {}",
                    maskedPan, vendorRes.getErrorDetails().getErrorCode(), vendorRes.getErrorDetails().getErrorMessage());

            ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), false);
            return PanVerificationResponse.builder()
                    .responseInfo(responseInfo)
                    .isVerified(false)
                    .maskedPan(maskedPan)
                    .source(source)
                    .isMock(isMock)
                    .errorDetails(PanVerificationResponse.PanErrorDetails.builder()
                            .errorCode(vendorRes.getErrorDetails().getErrorCode())
                            .errorMessage(vendorRes.getErrorDetails().getErrorMessage())
                            .build())
                    .build();
        }

        // 5. Enrich domain response from vendor details
        VendorPanVerificationResponse.ResponseParams vParams = vendorRes.getResponseParams();
        PanDetails uDetails = mapToPanDetails(vParams, maskedPan);
        boolean isVerified = vParams != null
                && PanConstants.STATUS_EXISTING_AND_VALID.equalsIgnoreCase(vParams.getPanStatus())
                && PanConstants.MATCH_YES.equalsIgnoreCase(vParams.getNameMatch())
                && PanConstants.MATCH_YES.equalsIgnoreCase(vParams.getDobMatch());

        ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), isVerified);
        PanVerificationResponse response = PanVerificationResponse.builder()
                .responseInfo(responseInfo)
                .isVerified(isVerified)
                .maskedPan(maskedPan)
                .source(source)
                .isMock(isMock)
                .panDetails(uDetails)
                .build();

        log.info("PAN verification completed in service for masked PAN: {}, isVerified: {}", maskedPan, isVerified);
        return response;
    }

    /**
     * Maps vendor-specific PAN response parameters into clean domain {@link PanDetails}.
     * Omits unmasked PAN, registered full name, and Aadhaar seeding status to protect citizen PII.
     *
     * @param vParams vendor response parameters
     * @param maskedPan masked PAN for telemetry
     * @return enriched domain {@link PanDetails}
     */
    private PanDetails mapToPanDetails(VendorPanVerificationResponse.ResponseParams vParams, String maskedPan) {
        // Guard check: return null if vendor returned no parameter details
        if (vParams == null) {
            return null;
        }

        boolean nameMatched = PanConstants.MATCH_YES.equalsIgnoreCase(vParams.getNameMatch());
        boolean dobMatched = PanConstants.MATCH_YES.equalsIgnoreCase(vParams.getDobMatch());

        return PanDetails.builder()
                .maskedPan(maskedPan)
                .category(vParams.getCategory())
                .panStatus(vParams.getPanStatus())
                .nameMatch(nameMatched)
                .dobMatch(dobMatched)
                .build();
    }
}
