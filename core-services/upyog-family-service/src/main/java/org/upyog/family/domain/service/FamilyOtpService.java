package org.upyog.family.domain.service;

import org.upyog.family.web.contract.FamilyOtpRequest;
import org.upyog.family.web.contract.FamilyOtpResponse;
import org.upyog.family.web.contract.FamilyOtpVerifyRequest;
import org.upyog.family.web.contract.FamilyOtpVerifyResponse;

/**
 * Domain service contract for Head of Family OTP dispatch, verification, and consent issuance in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyOtpService {

    /**
     * Initiates OTP dispatch to the Head of Family's mobile number for a citizen seeking to join the family.
     *
     * @param request the family OTP request containing family ID and tenant ID
     * @return response containing masked mobile number, reference ID, and dispatch status
     */
    FamilyOtpResponse sendFamilyOtp(FamilyOtpRequest request);

    /**
     * Verifies the OTP entered by citizen against Head of Family's phone and returns family details with consent token.
     *
     * @param request the family OTP verification request containing family ID, tenant ID, and OTP
     * @return response containing enriched family details, consent token, and verification status
     */
    FamilyOtpVerifyResponse verifyFamilyOtp(FamilyOtpVerifyRequest request);
}
