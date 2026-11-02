package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.web.contract.FamilyDashboardResponse;

/**
 * Service contract for aggregating household analytics and family dashboard metrics in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyDashboardService {

    /**
     * Aggregates family metrics including active members count, verification ratio, documents, and lineage events.
     *
     * @param familyId    the unique family identifier
     * @param tenantId    the tenant identifier
     * @param requestInfo UPYOG request context
     * @return FamilyDashboardResponse containing aggregated metrics
     */
    FamilyDashboardResponse getFamilyDashboard(String familyId, String tenantId, RequestInfo requestInfo);
}
