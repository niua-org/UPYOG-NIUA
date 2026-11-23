package org.egov.dx.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.util.GstConstants;
import org.egov.dx.util.GstNumberUtils;
import org.egov.dx.web.models.GstProfileRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

/**
 * Dedicated validator responsible for input sanity and format validation of GST requests.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b> by isolating all validation logic
 * away from the controller and service layers.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class GstVerificationValidator {

    /**
     * Validates incoming {@link GstProfileRequest}.
     *
     * @param request the profile search request
     * @throws CustomException if request, RequestInfo, or GSTIN format is invalid
     */
    public void validateGstRequest(GstProfileRequest request) {
        // Guard check: reject null payload to prevent NPE
        if (request == null) {
            log.error("GST verification validation failed: request is null");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, GstConstants.MSG_INVALID_REQUEST);
        }

        // eGov contract guard: RequestInfo is mandatory for user context and distributed tracing
        if (request.getRequestInfo() == null) {
            log.error("GST verification validation failed: RequestInfo is null");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, GstConstants.MSG_INVALID_REQUEST);
        }

        // Security check (Review comment High #1): Enforce authenticated user session with UUID
        // to prevent unauthorized automated scanning of business GST profiles
        if (request.getRequestInfo().getUserInfo() == null
                || StringUtils.isBlank(request.getRequestInfo().getUserInfo().getUuid())) {
            log.warn("GST verification validation failed: unauthenticated request missing userInfo");
            throw new CustomException(GstConstants.ERR_CODE_UNAUTHORIZED_USER, GstConstants.MSG_UNAUTHORIZED_USER);
        }

        String gstin = request.getGstin();
        // Input sanitization: ensure GSTIN string is present
        if (StringUtils.isBlank(gstin)) {
            log.warn("GST verification validation failed: GSTIN is blank");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_FORMAT, GstConstants.MSG_BLANK_GSTIN);
        }

        // Format & State code validation: check 15-char regex and state code (01-38, 97, 99)
        // to reject invalid syntax prior to vendor gateway dispatch
        if (!GstNumberUtils.isValidGstin(gstin, false)) {
            log.warn("GST verification validation failed for invalid format GSTIN: {}", GstNumberUtils.maskGstin(gstin));
            throw new CustomException(GstConstants.ERR_CODE_INVALID_FORMAT, GstConstants.MSG_INVALID_FORMAT);
        }

        // Checksum validation (Review comment High #6): Validate Mod-36 check digit on 15th character
        // to catch typos locally without wasting vendor API quota or introducing gateway latency
        if (!GstNumberUtils.validateCheckDigit(gstin)) {
            log.warn("GST verification validation failed for invalid checksum GSTIN: {}", GstNumberUtils.maskGstin(gstin));
            throw new CustomException(GstConstants.ERR_CODE_INVALID_CHECKSUM, GstConstants.MSG_INVALID_CHECKSUM);
        }

        // Multi-tenancy guard: tenantId is required for MDMS verification module enablement lookup
        if (StringUtils.isBlank(request.getTenantId())) {
            log.warn("GST verification validation failed: tenantId is blank");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, "Tenant ID is mandatory for GST verification.");
        }
    }
}
