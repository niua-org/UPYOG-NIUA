package org.upyog.family.persistence.repository;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.web.contract.CitizenProfile;

import java.util.List;
import java.util.Map;

/**
 * Client interface communicating with UPYOG user services to validate and enrich citizen profiles.
 * Follows Dependency Inversion Principle (DIP).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface UserClient {

    /**
     * Queries the UPYOG user service to retrieve citizen profiles by a list of citizen UUIDs.
     *
     * @param uuids list of citizen user UUIDs
     * @param requestInfo UPYOG request context envelope
     * @return map of citizen UUID to CitizenProfile object
     */
    Map<String, CitizenProfile> searchUsersByUuids(List<String> uuids, RequestInfo requestInfo);

    /**
     * Searches citizen profiles in the UPYOG user registry by 10-digit mobile number and tenant.
     *
     * @param mobileNumber citizen mobile number
     * @param tenantId tenant identifier
     * @param requestInfo UPYOG request context envelope
     * @return list of matching CitizenProfile objects
     */
    List<CitizenProfile> searchUsersByMobile(String mobileNumber, String tenantId, RequestInfo requestInfo);

    /**
     * Provisions a new citizen profile in the UPYOG user registry for a newly added family member.
     *
     * @param profile citizen profile attributes
     * @param requestInfo UPYOG request context envelope
     * @return created or local CitizenProfile instance with populated UUID
     */
    CitizenProfile createCitizenProfile(CitizenProfile profile, RequestInfo requestInfo);
}
