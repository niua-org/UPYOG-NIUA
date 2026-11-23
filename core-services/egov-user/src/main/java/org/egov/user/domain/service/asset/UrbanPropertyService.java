package org.egov.user.domain.service.asset;

import org.egov.user.web.contract.property.UrbanPropertyConsentValidateRequest;
import org.egov.user.web.contract.property.UrbanPropertyConsentValidateResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpResponse;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyRequest;
import org.egov.user.web.contract.property.UrbanPropertyOtpVerifyResponse;

/**
 * Service interface governing urban property ownership validation,
 * OTP dispatch, OTP verification, and consent validation.
 */
public interface UrbanPropertyService {

    /**
     * Enforces individual and multiple-owner property linking rules, then dispatches OTP
     * to the property owner's assigned mobile number.
     *
     * @param request OTP request containing propertyId, tenantId, and optional owner mobile
     * @return response containing referenceId and masked mobile number
     */
    UrbanPropertyOtpResponse sendUrbanPropertyOtp(UrbanPropertyOtpRequest request);

    /**
     * Verifies the submitted OTP and generates an authorization consent token bound to the propertyId.
     *
     * @param request OTP verify request containing propertyId, referenceId, and OTP
     * @return response containing verified consent token and expiry
     */
    UrbanPropertyOtpVerifyResponse verifyUrbanPropertyOtp(UrbanPropertyOtpVerifyRequest request);

    /**
     * Validates whether a given consent token is active and valid for the specified propertyId.
     *
     * @param request consent validate request
     * @return validation response
     */
    UrbanPropertyConsentValidateResponse validateConsentToken(UrbanPropertyConsentValidateRequest request);

    /**
     * Internal programmatic validation of property consent token.
     *
     * @param propertyId property ID
     * @param consentToken token string
     * @return true if valid and unexpired
     */
    boolean validatePropertyConsentToken(String propertyId, String consentToken);
}
