package org.egov.user.persistence.repository;

import org.egov.common.contract.request.RequestInfo;

import java.util.List;

/**
 * Interface for interacting with UPYOG IdGen service to produce formatted sequential business IDs in egov-user.
 * Follows Dependency Inversion Principle (DIP).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface IdGenRepository {

    /**
     * Calls UPYOG IdGen microservice to generate a formatted Citizen ID.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @return generated unique Citizen ID string
     */
    String generateCitizenId(RequestInfo requestInfo, String tenantId);

    /**
     * Calls UPYOG IdGen microservice to generate a formatted User Document ID.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @return generated unique User Document ID string
     */
    String generateUserDocumentId(RequestInfo requestInfo, String tenantId);

    /**
     * Generic bulk ID generation querying UPYOG IdGen microservice.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @param idName configured MDMS idName
     * @param format optional format override (if null/empty, idgen fetches from MDMS)
     * @param count number of IDs to generate
     * @return list of generated ID strings
     */
    List<String> getIdList(RequestInfo requestInfo, String tenantId, String idName, String format, int count);
}
