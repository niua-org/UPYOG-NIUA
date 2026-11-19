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
        if (request == null) {
            log.error("GST verification validation failed: request is null");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, GstConstants.MSG_INVALID_REQUEST);
        }

        if (request.getRequestInfo() == null) {
            log.error("GST verification validation failed: RequestInfo is null");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, GstConstants.MSG_INVALID_REQUEST);
        }

        String gstin = request.getGstin();
        if (StringUtils.isBlank(gstin)) {
            log.warn("GST verification validation failed: GSTIN is blank");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_FORMAT, GstConstants.MSG_BLANK_GSTIN);
        }

        if (!GstNumberUtils.isValidGstin(gstin)) {
            log.warn("GST verification validation failed for invalid format GSTIN: {}", GstNumberUtils.maskGstin(gstin));
            throw new CustomException(GstConstants.ERR_CODE_INVALID_FORMAT, GstConstants.MSG_INVALID_FORMAT);
        }

        if (StringUtils.isBlank(request.getTenantId())) {
            log.warn("GST verification validation failed: tenantId is blank");
            throw new CustomException(GstConstants.ERR_CODE_INVALID_REQUEST, "Tenant ID is mandatory for GST verification.");
        }
    }
}
