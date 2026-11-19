package org.egov.dx.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.web.models.UserDemographics;
import org.egov.dx.web.models.UserRegistryRecord;

/**
 * Service abstraction for interacting with egov-user service to resolve existing citizen profiles
 * or auto-provisioning new verified citizen profiles upon successful Aadhaar verification.
 *
 * <p>Adheres strictly to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles citizen profile lookups and provisioning in egov-user.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Controllers depend on this interface, not the concrete HTTP client.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface UserRegistryService {

    /**
     * Resolves the citizen UUID for a given Aadhaar number (raw or masked) by querying egov-user.
     *
     * <p>Note: Thin wrapper over {@link #searchCitizenByAadhaar(String, String, RequestInfo)};
     * callers needing multiple attributes should prefer the record API to avoid duplicate HTTP roundtrips.</p>
     *
     * @param aadhaarNumber the raw 12-digit or masked Aadhaar number
     * @param tenantId      the tenant identifier (e.g., "pg.citya", "pg")
     * @param requestInfo   eGov RequestInfo header
     * @return the citizen UUID if found, or null if citizen does not exist or lookup fails
     */
    String searchUserUuidByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo);

    /**
     * Resolves citizen account record (user UUID and active familyId) for a given Aadhaar number by querying egov-user.
     *
     * @param aadhaarNumber the raw 12-digit or masked Aadhaar number
     * @param tenantId      the tenant identifier
     * @param requestInfo   eGov RequestInfo header
     * @return {@link UserRegistryRecord} containing userUuid and active familyId if found, or null
     */
    UserRegistryRecord searchCitizenByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo);

    /**
     * Resolves the active Family ID for a given Aadhaar number by querying egov-user / family service.
     *
     * <p>Note: Thin wrapper over {@link #searchCitizenByAadhaar(String, String, RequestInfo)};
     * callers should prefer {@link #searchCitizenByAadhaar(String, String, RequestInfo)} to avoid duplicate HTTP calls.</p>
     *
     * @param aadhaarNumber the raw 12-digit or masked Aadhaar number
     * @param tenantId      the tenant identifier
     * @param requestInfo   eGov RequestInfo header
     * @return active familyId string, or null if not found
     */
    String searchFamilyIdByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo);

    /**
     * Resolves the active Family ID for a citizen user UUID by querying family service or egov-user.
     *
     * @param userUuid    citizen unique identifier
     * @param tenantId    the tenant identifier
     * @param requestInfo eGov RequestInfo header
     * @return active familyId string, or null if not found
     */
    String searchFamilyIdByUserUuid(String userUuid, String tenantId, RequestInfo requestInfo);

    /**
     * Resolves an existing citizen UUID by Aadhaar; if citizen does not exist in egov-user,
     * auto-provisions a new verified citizen profile using the verified e-KYC demographics
     * and returns the newly generated UUID.
     *
     * @param aadhaarNumber the raw 12-digit or masked Aadhaar number
     * @param demographics  verified demographic details from e-KYC
     * @param tenantId      the tenant identifier
     * @param requestInfo   eGov RequestInfo header
     * @return the existing or newly created citizen UUID
     */
    String resolveOrCreateUserUuid(String aadhaarNumber, UserDemographics demographics, String tenantId, RequestInfo requestInfo);

    /**
     * Marks an existing citizen as identity-verified after successful Aadhaar e-KYC OTP verification.
     * Persists full 12-digit Aadhaar via egov-user profile service (inter-service).
     *
     * @param userUuid      citizen UUID to update
     * @param aadhaarNumber verified 12-digit Aadhaar number
     * @param tenantId      tenant identifier
     * @param requestInfo   request context
     */
    void markCitizenAadhaarVerifiedAfterEkyc(String userUuid, String aadhaarNumber, String tenantId, RequestInfo requestInfo);
}
