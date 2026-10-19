package org.egov.user.domain.service.multiauth.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.tracer.model.CustomException;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.multiauth.AuthType;
import org.egov.user.web.contract.multiauth.LoginV2Request;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of {@link LoginV2Validator} verifying business constraints and parameter integrity.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class LoginV2ValidatorImpl implements LoginV2Validator {

    @Override
    public void validate(LoginV2Request request) {
        if (request == null) {
            log.error("[LOGIN-V2-VALIDATOR] Validation failed: request body is null");
            throw new CustomException(UserConstants.ERROR_INVALID_REQUEST, UserConstants.MSG_LOGIN_REQUEST_NULL);
        }

        if (StringUtils.isBlank(request.getTenantId())) {
            log.error("[LOGIN-V2-VALIDATOR] Validation failed: tenantId is missing");
            throw new CustomException(UserConstants.ERROR_INVALID_TENANT_ID, UserConstants.MSG_LOGIN_TENANT_REQUIRED);
        }

        if (StringUtils.isBlank(request.getOtp())) {
            log.error("[LOGIN-V2-VALIDATOR] Validation failed: otp is missing for tenant: {}", request.getTenantId());
            throw new CustomException(UserConstants.ERROR_INVALID_OTP, UserConstants.MSG_LOGIN_OTP_REQUIRED);
        }

        AuthType authType = request.getAuthType();
        if (authType != null) {
            validateChannelSpecificRequirements(request, authType);
        }

        log.debug("[LOGIN-V2-VALIDATOR] Validation passed successfully for tenant: {}, authType: {}",
                request.getTenantId(), authType);
    }

    private void validateChannelSpecificRequirements(LoginV2Request request, AuthType authType) {
        switch (authType) {
            case MOBILE:
                if (StringUtils.isBlank(request.getMobileNumber())) {
                    log.error("[LOGIN-V2-VALIDATOR] Validation failed: mobileNumber required for MOBILE auth");
                    throw new CustomException(UserConstants.ERROR_INVALID_MOBILE_NUMBER, UserConstants.MSG_MOBILE_AUTH_MOBILE_REQUIRED);
                }
                break;

            case AADHAAR:
                boolean hasAadhaarIdentifier = StringUtils.isNotBlank(request.getAadhaarNumber())
                        || StringUtils.isNotBlank(request.getTxnId())
                        || StringUtils.isNotBlank(request.getReferenceId());
                if (!hasAadhaarIdentifier) {
                    log.error("[LOGIN-V2-VALIDATOR] Validation failed: Aadhaar identifier required for AADHAAR auth");
                    throw new CustomException(UserConstants.ERROR_INVALID_AADHAAR_IDENTIFIER,
                            UserConstants.MSG_AADHAAR_IDENTIFIER_REQUIRED);
                }
                break;

            case FAMILY_ID:
                if (StringUtils.isBlank(request.getFamilyId())) {
                    log.error("[LOGIN-V2-VALIDATOR] Validation failed: familyId required for FAMILY_ID auth");
                    throw new CustomException(UserConstants.ERROR_INVALID_FAMILY_ID, UserConstants.MSG_FAMILY_ID_REQUIRED);
                }
                if (StringUtils.isBlank(request.getMobileNumber())) {
                    log.error("[LOGIN-V2-VALIDATOR] Validation failed: member mobileNumber required for FAMILY_ID auth");
                    throw new CustomException(UserConstants.ERROR_INVALID_MOBILE_NUMBER, UserConstants.MSG_FAMILY_MEMBER_MOBILE_REQUIRED);
                }
                break;
        }
    }
}
