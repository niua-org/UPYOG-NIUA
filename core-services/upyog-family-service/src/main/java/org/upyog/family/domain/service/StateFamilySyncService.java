package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.state.StateFamilyJitProvisionRequest;
import org.upyog.family.web.contract.state.StateFamilySyncRequest;
import org.upyog.family.web.contract.state.StateFamilySyncResponse;

/**
 * Service interface managing external State Family registry integration (UC2).
 * Handles State wrapper data ingestion, "Family Found" UI presentation, and Just-In-Time (JIT) provisioning.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface StateFamilySyncService {

    /**
     * Ingests and stages the State Family & members payload received from the State Wrapper.
     * Generates a unique referenceId for the UI to consume.
     *
     * @param request the state family sync request
     * @return StateFamilySyncResponse with referenceId and masked member identifiers
     */
    StateFamilySyncResponse syncStateFamily(StateFamilySyncRequest request);

    /**
     * Retrieves the staged State Family details by referenceId or stateFamilyId for the "Family Found" UI screen.
     *
     * @param referenceId the unique reference UUID
     * @param stateFamilyId state household identifier
     * @param requestInfo UPYOG request context envelope
     * @return StateFamilySyncResponse with masked demographics
     */
    StateFamilySyncResponse getStateFamily(String referenceId, String stateFamilyId, RequestInfo requestInfo);

    /**
     * Executes the Just-In-Time (JIT) check and provisioning cascade upon member OTP verification:
     * 1. Checks if family exists in UPYOG. If not, provisions family unit.
     * 2. Maps the authenticated citizen userUuid to the family member record.
     *
     * @param request JIT provisioning request containing userUuid and state identifiers
     * @return active Family aggregate in UPYOG
     */
    Family jitProvisionFamilyAndMember(StateFamilyJitProvisionRequest request);
}
