package org.egov.user.domain.service.family;

import org.egov.user.web.contract.family.FamilyProfileResponse;
import org.egov.user.web.contract.family.FamilyProfileSearchRequest;

/**
 * Service abstraction for retrieving complete family household information,
 * including member demographic profiles, identity proofs, and addresses.
 *
 * <p>Strictly adheres to SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Exclusively manages family-scoped profile and asset retrieval.</li>
 *   <li><b>Interface Segregation Principle (ISP):</b> Dedicated contract decoupling family aggregation from generic user operations.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> High-level controllers depend on this abstraction.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyProfileService {

    /**
     * Searches complete family household details by familyId, resolving active members
     * from upyog-family-service and enriching each member with demographics, addresses, and documents
     * from egov-user core registry.
     *
     * @param request family profile search request payload containing familyId, optional tenantId, and RequestInfo
     * @return {@link FamilyProfileResponse} containing the complete family unit aggregate with enriched members
     * @throws org.egov.tracer.model.CustomException if validation fails, family is not found, or access is unauthorized
     */
    FamilyProfileResponse searchFamilyProfile(FamilyProfileSearchRequest request);
}
