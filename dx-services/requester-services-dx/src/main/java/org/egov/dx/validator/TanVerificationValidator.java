package org.egov.dx.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.util.TanConstants;
import org.egov.dx.util.TanNumberUtils;
import org.egov.dx.web.models.TanVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

/**
 * Dedicated validator responsible for validation of TAN verification requests.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b> by isolating format and business validation
 * away from the HTTP controller orchestration.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class TanVerificationValidator {

    /**
     * Validates incoming {@link TanVerificationRequest}.
     *
     * @param request the verification request
     * @throws CustomException if request, RequestInfo, or TAN format is invalid
     */
    public void validateTanRequest(TanVerificationRequest request) {
        // Guard check: reject null payload to prevent NPE
        if (request == null) {
            log.error("TAN verification validation failed: request is null");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, TanConstants.MSG_INVALID_REQUEST);
        }

        // eGov contract guard: RequestInfo is mandatory for user auditing and request tracking
        if (request.getRequestInfo() == null) {
            log.error("TAN verification validation failed: RequestInfo is null");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, TanConstants.MSG_INVALID_REQUEST);
        }

        // Security check (Review comment High #1): Mandate authenticated session with user UUID
        // to prevent unauthenticated access to employer/deductor tax data
        if (request.getRequestInfo().getUserInfo() == null
                || StringUtils.isBlank(request.getRequestInfo().getUserInfo().getUuid())) {
            log.warn("TAN verification validation failed: unauthenticated request missing userInfo");
            throw new CustomException(TanConstants.ERR_CODE_UNAUTHORIZED_USER, TanConstants.MSG_UNAUTHORIZED_USER);
        }

        String tan = request.getTan();
        // Input sanitization: ensure TAN string is provided
        if (StringUtils.isBlank(tan)) {
            log.warn("TAN verification validation failed: TAN is blank");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_FORMAT, TanConstants.MSG_BLANK_TAN);
        }

        // Structural syntax validation: verify TAN conforms to Indian ITD 10-char format ([A-Z]{4}[0-9]{5}[A-Z])
        // to reject invalid syntax prior to vendor gateway dispatch
        if (!TanNumberUtils.isValidTan(tan)) {
            log.warn("TAN verification validation failed for invalid format TAN: {}", TanNumberUtils.maskTan(tan));
            throw new CustomException(TanConstants.ERR_CODE_INVALID_FORMAT, TanConstants.MSG_INVALID_FORMAT);
        }

        // Optional deductorName validation: if supplied, protect against buffer overflow or payload abuse (>200 chars)
        if (StringUtils.isNotBlank(request.getDeductorName()) && request.getDeductorName().trim().length() > 200) {
            log.warn("TAN verification validation failed: deductorName exceeds 200 characters");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "Deductor name cannot exceed 200 characters.");
        }

        // Multi-tenancy guard: tenantId is mandatory to determine tenant verification entitlement via MDMS
        if (StringUtils.isBlank(request.getTenantId())) {
            log.warn("TAN verification validation failed: tenantId is blank");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "Tenant ID is mandatory for TAN verification.");
        }
    }
}
