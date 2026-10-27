package org.upyog.family.persistence.repository;

import org.egov.common.contract.request.RequestInfo;

/**
 * Interface for interacting with UPYOG IdGen service to produce formatted family IDs.
 * Follows Dependency Inversion Principle (DIP).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface IdGenRepository {

    /**
     * Calls UPYOG IdGen microservice to generate a formatted, unique sequential Family ID.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @return generated unique Family ID string
     */
    String generateFamilyId(RequestInfo requestInfo, String tenantId);
}
