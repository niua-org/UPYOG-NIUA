package org.egov.dx.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.dx.util.PanConstants;
import org.egov.dx.util.PanNumberUtils;
import org.egov.dx.web.models.PanVerificationRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

/**
 * Dedicated validator responsible for input sanity and format validation of PAN requests.
 *
 * <p>Enforces the <b>Single Responsibility Principle (SRP)</b> by isolating all validation logic
 * away from the controller and service layers.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class PanVerificationValidator {

    /**
     * Validates incoming {@link PanVerificationRequest}.
     *
     * @param request the verification request
     * @throws CustomException if request, RequestInfo, PAN format, fullName, or dob is invalid
     */
    public void validatePanRequest(PanVerificationRequest request) {
        if (request == null) {
            log.error("PAN verification validation failed: request is null");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, PanConstants.MSG_INVALID_REQUEST);
        }

        if (request.getRequestInfo() == null) {
            log.error("PAN verification validation failed: RequestInfo is null");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, PanConstants.MSG_INVALID_REQUEST);
        }

        String pan = request.getPan();
        if (StringUtils.isBlank(pan)) {
            log.warn("PAN verification validation failed: PAN is blank");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_FORMAT, PanConstants.MSG_BLANK_PAN);
        }

        if (!PanNumberUtils.isValidPan(pan)) {
            log.warn("PAN verification validation failed for invalid format PAN: {}", PanNumberUtils.maskPan(pan));
            throw new CustomException(PanConstants.ERR_CODE_INVALID_FORMAT, PanConstants.MSG_INVALID_FORMAT);
        }

        if (StringUtils.isBlank(request.getFullName())) {
            log.warn("PAN verification validation failed: fullName is blank for masked PAN: {}", PanNumberUtils.maskPan(pan));
            throw new CustomException(PanConstants.ERR_CODE_MISSING_NAME, PanConstants.MSG_MISSING_NAME);
        }

        if (StringUtils.isBlank(request.getDob())) {
            log.warn("PAN verification validation failed: dob is blank for masked PAN: {}", PanNumberUtils.maskPan(pan));
            throw new CustomException(PanConstants.ERR_CODE_MISSING_DOB, PanConstants.MSG_MISSING_DOB);
        }

        if (StringUtils.isBlank(request.getTenantId())) {
            log.warn("PAN verification validation failed: tenantId is blank");
            throw new CustomException(PanConstants.ERR_CODE_INVALID_REQUEST, "Tenant ID is mandatory for PAN verification.");
        }
    }
}
