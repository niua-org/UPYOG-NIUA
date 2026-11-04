package org.egov.user.domain.service.profile.validator;

import org.egov.user.domain.model.User;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.egov.user.web.contract.profile.ProfileInitRequest;
import org.egov.user.web.contract.profile.ProfileResyncRequest;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;

/**
 * Validation contract for profile operations enforcing Section 8 mandatory fields
 * and non-editable field restrictions.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Exclusively responsible for validating profile payloads and business integrity.</li>
 *   <li><b>Interface Segregation Principle (ISP):</b> Segregates profile validation operations into dedicated lifecycle methods.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface ProfileValidator {

    /**
     * Validates profile initialization parameters (tenantId and mobileNumber).
     *
     * @param request profile init request payload
     * @throws org.egov.tracer.model.CustomException if request is null or required fields are missing
     */
    void validateInit(ProfileInitRequest request);

    /**
     * Validates that all Section 8 mandatory fields are present for citizen profile creation.
     * Mandatory: Name, Father's Name, Mother's Name, Mobile Number, DOB, Email ID, Gender,
     * Address (City, Locality, Street Name, House Number, Landmark, Pin Code, Address Type),
     * and Documents (Address Proof, Identity Proof, DOB Proof).
     *
     * @param request profile create request
     * @throws org.egov.tracer.model.CustomException if any mandatory demographic, address, or document field is missing
     */
    void validateCreate(ProfileCreateRequest request);

    /**
     * Validates profile update parameters and enforces non-editable restrictions
     * (locking Name, Father's Name, Mother's Name, Mobile Number, DOB, Email ID, Gender).
     *
     * @param request      profile update request
     * @param existingUser existing user in database
     * @throws org.egov.tracer.model.CustomException if non-editable fields are illegally modified or data mismatch occurs
     */
    void validateUpdate(ProfileUpdateRequest request, User existingUser);

    /**
     * Validates profile search criteria ensuring at least one valid search parameter is supplied.
     *
     * @param request profile search request
     * @throws org.egov.tracer.model.CustomException if search request is empty or criteria missing
     */
    void validateSearch(ProfileSearchRequest request);

    /**
     * Validates profile re-sync request ensuring OTP and tenantId are present and target user exists.
     *
     * @param request      profile re-sync request
     * @param existingUser existing user in database
     * @throws org.egov.tracer.model.CustomException if OTP is missing or target user record does not exist
     */
    void validateResync(ProfileResyncRequest request, User existingUser);
}
