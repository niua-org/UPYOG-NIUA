package org.egov.dx.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.web.models.UserDemographics;

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
     * @param aadhaarNumber the raw 12-digit or masked Aadhaar number
     * @param tenantId      the tenant identifier (e.g., "pg.citya", "pg")
     * @param requestInfo   eGov RequestInfo header
     * @return the citizen UUID if found, or null if citizen does not exist or lookup fails
     */
    String searchUserUuidByAadhaar(String aadhaarNumber, String tenantId, RequestInfo requestInfo);

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
}
