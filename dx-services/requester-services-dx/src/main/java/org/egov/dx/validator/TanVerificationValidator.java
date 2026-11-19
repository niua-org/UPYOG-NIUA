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
        if (request == null) {
            log.error("TAN verification validation failed: request is null");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, TanConstants.MSG_INVALID_REQUEST);
        }

        if (request.getRequestInfo() == null) {
            log.error("TAN verification validation failed: RequestInfo is null");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, TanConstants.MSG_INVALID_REQUEST);
        }

        String tan = request.getTan();
        if (StringUtils.isBlank(tan)) {
            log.warn("TAN verification validation failed: TAN is blank");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_FORMAT, TanConstants.MSG_BLANK_TAN);
        }

        if (!TanNumberUtils.isValidTan(tan)) {
            log.warn("TAN verification validation failed for invalid format TAN: {}", TanNumberUtils.maskTan(tan));
            throw new CustomException(TanConstants.ERR_CODE_INVALID_FORMAT, TanConstants.MSG_INVALID_FORMAT);
        }

        if (StringUtils.isBlank(request.getTenantId())) {
            log.warn("TAN verification validation failed: tenantId is blank");
            throw new CustomException(TanConstants.ERR_CODE_INVALID_REQUEST, "Tenant ID is mandatory for TAN verification.");
        }
    }
}
