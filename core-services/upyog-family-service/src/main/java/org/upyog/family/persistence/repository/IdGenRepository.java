package org.upyog.family.persistence.repository;

import org.egov.common.contract.request.RequestInfo;

import java.util.List;

/**
 * Interface for interacting with UPYOG IdGen service to produce formatted family and member IDs.
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

    /**
     * Calls UPYOG IdGen microservice to generate a formatted, unique sequential Family Member ID.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @return generated unique Family Member ID string
     */
    String generateMemberId(RequestInfo requestInfo, String tenantId);

    /**
     * Calls UPYOG IdGen microservice in bulk to generate formatted, unique sequential Family Member IDs.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @param count number of member IDs to generate
     * @return list of generated unique Family Member ID strings
     */
    List<String> generateMemberIds(RequestInfo requestInfo, String tenantId, int count);

    /**
     * Generic bulk ID generation method querying UPYOG IdGen microservice.
     *
     * @param requestInfo UPYOG request context envelope
     * @param tenantId target tenant identifier
     * @param idName configured MDMS idName
     * @param format optional format template override (if null/empty, idgen fetches from MDMS)
     * @param count number of IDs to generate
     * @return list of generated ID strings
     */
    List<String> getIdList(RequestInfo requestInfo, String tenantId, String idName, String format, int count);
}
