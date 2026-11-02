package org.egov.user.domain.service.profile;

import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.User;
import org.egov.user.web.contract.profile.ProfileCreateRequest;
import org.egov.user.web.contract.profile.ProfileIncludeEntity;
import org.egov.user.web.contract.profile.ProfileInitRequest;
import org.egov.user.web.contract.profile.ProfileResyncRequest;
import org.egov.user.web.contract.profile.ProfileSearchRequest;
import org.egov.user.web.contract.profile.ProfileUpdateRequest;

import java.util.List;
import java.util.Set;

/**
 * Service contract for citizen profile lifecycle management, supporting full profile creation,
 * lightweight onboarding, demographic updates with non-editable restrictions, search with entity enrichment,
 * and Aadhaar profile re-sync.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Defines the contract for citizen profile lifecycle operations.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> High-level controllers depend on this abstraction rather than concrete implementations.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface ProfileService {

    /**
     * Creates a complete citizen profile enforcing all Section 8 mandatory fields
     * (demographics, address, and document proofs).
     *
     * @param request profile create request payload
     * @return created citizen user profile with decrypted details
     * @throws org.egov.tracer.model.CustomException if mandatory fields are missing or citizen already exists
     */
    User createProfile(ProfileCreateRequest request);

    /**
     * Initializes a lightweight citizen profile with minimum required fields.
     *
     * @param request profile init request payload
     * @return initialized or existing citizen user profile with decrypted details
     * @throws org.egov.tracer.model.CustomException if required fields are missing or invalid
     */
    User initProfile(ProfileInitRequest request);

    /**
     * Updates citizen profile with Section 8 non-editable field protection and version tracking.
     * Core identity fields can only be modified if authenticated via Aadhaar re-sync.
     *
     * @param request profile update request payload
     * @return updated citizen user profile with decrypted details
     * @throws org.egov.tracer.model.CustomException if non-editable fields are modified without re-sync
     */
    User updateProfile(ProfileUpdateRequest request);

    /**
     * Searches citizen profiles by criteria with decrypted PII attributes, resolving requested entity sections.
     *
     * @param request profile search request payload
     * @return list of matching decrypted citizen profiles
     * @throws org.egov.tracer.model.CustomException if search criteria are empty or malformed
     */
    List<User> searchProfile(ProfileSearchRequest request);

    /**
     * Searches citizen profiles by criteria with decrypted PII attributes using pre-resolved entity inclusion flags.
     *
     * @param request  profile search request payload
     * @param includes pre-resolved set of {@link ProfileIncludeEntity} sections to enrich
     * @return list of matching decrypted citizen profiles
     * @throws org.egov.tracer.model.CustomException if search criteria are empty or malformed
     */
    List<User> searchProfile(ProfileSearchRequest request, Set<ProfileIncludeEntity> includes);

    /**
     * Resolves and normalizes requested entity inclusion flags (PROFILE, ADDRESS, DOCUMENT).
     * Validates each section using {@link ProfileIncludeEntity#fromString(String)} and rejects unknown
     * sections with an HTTP 400 {@link CustomException}. Defaults to [PROFILE] if omitted or empty.
     *
     * @param rawIncludes raw include flag strings from request (e.g. "PROFILE", "ADDRESS", "DOCUMENT")
     * @return set of validated {@link ProfileIncludeEntity} include flags
     * @throws org.egov.tracer.model.CustomException if an unknown include entity string is provided
     */
    Set<ProfileIncludeEntity> resolveIncludes(List<String> rawIncludes);

    /**
     * Re-synchronizes citizen profile demographics with verified Aadhaar e-KYC.
     *
     * @param request profile re-sync request payload
     * @return refreshed citizen user profile with decrypted details
     * @throws org.egov.tracer.model.CustomException if OTP verification fails or target citizen does not exist
     */
    User resyncProfile(ProfileResyncRequest request);
}
