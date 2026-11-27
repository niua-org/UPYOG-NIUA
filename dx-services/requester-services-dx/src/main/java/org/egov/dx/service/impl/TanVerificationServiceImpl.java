package org.egov.dx.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationRequest;
import org.egov.dx.adapter.vendor.dto.VendorTanVerificationResponse;
import org.egov.dx.adapter.vendor.tan.TanGatewayAdapter;
import org.egov.dx.service.TanVerificationService;
import org.egov.dx.service.TenantVerificationConfigService;
import org.egov.dx.util.TanConstants;
import org.egov.dx.util.TanNumberUtils;
import org.egov.dx.validator.TanVerificationValidator;
import org.egov.dx.web.models.ResponseInfoFactory;
import org.egov.dx.web.models.TanDetails;
import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.dx.web.models.TanVerificationResponse;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of {@link TanVerificationService} orchestrating validation, vendor transformation,
 * gateway execution, and domain response enrichment.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles TAN verification domain business logic, separating it from the HTTP transport controller.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects polymorphic {@link TanGatewayAdapter} abstraction.</li>
 *   <li><b>Open/Closed Principle (OCP):</b> Vendor schema changes are isolated to this service layer without polluting REST controllers.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class TanVerificationServiceImpl implements TanVerificationService {

    private final TanGatewayAdapter gatewayAdapter;
    private final TanVerificationValidator validator;
    private final TenantVerificationConfigService tenantConfigService;

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param gatewayAdapter abstraction for active vendor/mock TAN gateway adapter
     * @param validator component handling request validation
     * @param tenantConfigService component checking tenant-level MDMS enablement
     */
    @Autowired
    public TanVerificationServiceImpl(TanGatewayAdapter gatewayAdapter,
                                      TanVerificationValidator validator,
                                      TenantVerificationConfigService tenantConfigService) {
        this.gatewayAdapter = gatewayAdapter;
        this.validator = validator;
        this.tenantConfigService = tenantConfigService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates request, enforces MDMS tenant configuration, delegates to TAN adapter,
     * performs deductor name matching if supplied, and returns verification response.</p>
     */
    @Override
    public TanVerificationResponse verifyTan(TanVerificationRequest request) {
        // 1. Validate incoming request (SRP)
        validator.validateTanRequest(request);

        // Multi-tenant authorization guard (Review comment Medium #3): Verify that the tenant/ULB
        // has enabled the TAN verification module in MDMS configuration before invoking external APIs
        if (!tenantConfigService.isVerificationEnabled(request.getRequestInfo(), request.getTenantId(), "TAN")) {
            log.warn("TAN verification is not enabled in MDMS for tenant: {}", request.getTenantId());
            throw new CustomException(TanConstants.ERR_CODE_TENANT_DISABLED,
                    "TAN verification is not enabled for tenant: " + request.getTenantId() + " in MDMS configuration.");
        }

        String rawTan = request.getTan();
        String normalizedTan = TanNumberUtils.normalizeTan(rawTan);
        String maskedTan = TanNumberUtils.maskTan(normalizedTan);

        log.debug("Processing TAN verification in service for masked TAN: {}, apiId: {}",
                maskedTan, request.getRequestInfo() != null ? request.getRequestInfo().getApiId() : "null");
        log.info("Processing TAN verification in service for masked TAN: {}, tenantId: {}",
                maskedTan, request.getTenantId());

        String requestId = (request.getRequestInfo() != null && StringUtils.isNotBlank(request.getRequestInfo().getMsgId()))
                ? request.getRequestInfo().getMsgId()
                : UUID.randomUUID().toString();

        // 2. Transform domain request to vendor DTO
        VendorTanVerificationRequest vendorReq = VendorTanVerificationRequest.builder()
                .header(VendorTanVerificationRequest.RequestHeader.builder()
                        .requestId(requestId)
                        .serviceId(TanConstants.DEFAULT_SERVICE_ID)
                        .timestamp(Instant.now().toString())
                        .version(TanConstants.DEFAULT_VERSION)
                        .build())
                .searchCriteria(VendorTanVerificationRequest.SearchCriteria.builder()
                        .tan(normalizedTan)
                        .build())
                .build();

        // 3. Delegate to gateway adapter abstraction (DIP)
        VendorTanVerificationResponse vendorRes = gatewayAdapter.verifyTan(vendorReq);

        // Resilience guard: handle null return from adapter (e.g., unexpected network failure or unhandled exception)
        if (vendorRes == null) {
            log.error("TAN Gateway returned null response for masked TAN: {}", maskedTan);
            throw new CustomException(TanConstants.ERR_CODE_GATEWAY_ERROR, TanConstants.MSG_GATEWAY_ERROR);
        }

        boolean isMock = gatewayAdapter.getClass().getSimpleName().startsWith("Mock");
        String source = isMock ? TanConstants.SOURCE_MOCK : TanConstants.SOURCE_LIVE_ITD;

        // 4. Handle vendor-level error payload (e.g. record not found or invalid credentials)
        if (vendorRes.getErrorDetails() != null) {
            log.warn("ITD TAN gateway returned error for masked TAN {}: {} - {}",
                    maskedTan, vendorRes.getErrorDetails().getErrorCode(), vendorRes.getErrorDetails().getErrorMessage());

            ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), false);
            return TanVerificationResponse.builder()
                    .responseInfo(responseInfo)
                    .isVerified(false)
                    .maskedTan(maskedTan)
                    .source(source)
                    .isMock(isMock)
                    .errorDetails(TanVerificationResponse.TanErrorDetails.builder()
                            .errorCode(vendorRes.getErrorDetails().getErrorCode())
                            .errorMessage(vendorRes.getErrorDetails().getErrorMessage())
                            .build())
                    .build();
        }

        // 5. Enrich domain response from vendor details
        VendorTanVerificationResponse.VendorTanDetails vDetails = vendorRes.getTanDetails();
        TanDetails uDetails = mapToTanDetails(vDetails, maskedTan);
        boolean isVerified = vDetails != null && TanConstants.STATUS_ACTIVE.equalsIgnoreCase(vDetails.getStatus());

        // Deductor name matching: if caller supplied deductorName, compare with official registered name
        if (vDetails != null && uDetails != null && StringUtils.isNotBlank(request.getDeductorName())) {
            boolean nameMatched = evaluateDeductorNameMatch(request.getDeductorName(), vDetails.getNameOfDeductor());
            uDetails.setNameMatch(nameMatched);
            isVerified = isVerified && nameMatched;
            log.info("TAN deductor name match evaluated for masked TAN: {}, nameMatched: {}", maskedTan, nameMatched);
        }

        ResponseInfo responseInfo = ResponseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), isVerified);
        TanVerificationResponse response = TanVerificationResponse.builder()
                .responseInfo(responseInfo)
                .isVerified(isVerified)
                .maskedTan(maskedTan)
                .source(source)
                .isMock(isMock)
                .tanDetails(uDetails)
                .build();

        log.info("TAN verification completed in service for masked TAN: {}, isVerified: {}", maskedTan, isVerified);
        return response;
    }

    private boolean evaluateDeductorNameMatch(String inputName, String recordName) {
        // Missing name guard: if input or record deductor name is missing, cannot match
        if (StringUtils.isBlank(inputName) || StringUtils.isBlank(recordName)) {
            return false;
        }
        String cleanInput = inputName.trim().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").toUpperCase();
        String cleanRecord = recordName.trim().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").toUpperCase();
        return cleanInput.equalsIgnoreCase(cleanRecord);
    }

    /**
     * Maps vendor-specific TAN details into clean domain {@link TanDetails}.
     *
     * @param vDetails vendor details DTO
     * @param maskedTan masked TAN for telemetry
     * @return enriched domain {@link TanDetails}
     */
    private TanDetails mapToTanDetails(VendorTanVerificationResponse.VendorTanDetails vDetails, String maskedTan) {
        // Defensive check: return null if vendor returned no details payload
        if (vDetails == null) {
            return null;
        }

        TanDetails.TanAddress addr = null;
        // Address check: map address fields if provided in vendor response
        if (vDetails.getAddress() != null) {
            addr = TanDetails.TanAddress.builder()
                    .line1(vDetails.getAddress().getLine1())
                    .line2(vDetails.getAddress().getLine2())
                    .city(vDetails.getAddress().getCity())
                    .state(vDetails.getAddress().getState())
                    .pinCode(vDetails.getAddress().getPinCode())
                    .build();
        }

        TanDetails.TanJurisdiction jur = null;
        // Jurisdiction check: map assessing officer (AO) details if provided in vendor response
        if (vDetails.getJurisdictionDetails() != null) {
            jur = TanDetails.TanJurisdiction.builder()
                    .aoCode(vDetails.getJurisdictionDetails().getAoCode())
                    .aoType(vDetails.getJurisdictionDetails().getAoType())
                    .rangeCode(vDetails.getJurisdictionDetails().getRangeCode())
                    .aoNumber(vDetails.getJurisdictionDetails().getAoNumber())
                    .description(vDetails.getJurisdictionDetails().getDescription())
                    .build();
        }

        return TanDetails.builder()
                .maskedTan(maskedTan)
                .nameOfDeductor(vDetails.getNameOfDeductor())
                .categoryOfDeductor(vDetails.getCategoryOfDeductor())
                .status(vDetails.getStatus())
                .emailId(vDetails.getEmailId())
                .address(addr)
                .jurisdictionDetails(jur)
                .build();
    }
}
