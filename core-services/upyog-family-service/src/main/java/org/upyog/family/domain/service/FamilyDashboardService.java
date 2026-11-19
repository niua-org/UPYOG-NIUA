package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.web.contract.FamilyDashboardResponse;

/**
 * Domain service contract for aggregating household analytics, demographic ratios, and dashboard metrics in UPYOG.
 *
 * <p>Provides consolidated statistical insights for citizen portals and municipal dashboards including total active
 * members, verification percentage, document compliance counts, and lineage genealogical audit history.</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyDashboardService {

    /**
     * Aggregates family metrics including active members count, verification ratio, documents, and lineage events.
     *
     * <p>Computes demographic distribution, identifies the active Head of Family, counts attached verification proofs,
     * and maps genealogical lineage history (split and merge events) into a unified dashboard summary.</p>
     *
     * @param familyId the unique family identifier; must not be blank.
     * @param tenantId the tenant identifier scoping the query; must not be blank.
     * @param requestInfo UPYOG request context envelope containing auth token; may be {@code null}.
     * @return {@link FamilyDashboardResponse} containing calculated metrics and audit summaries.
     * @throws org.egov.tracer.model.CustomException if the family is not found (Code: {@code FAMILY_NOT_FOUND})
     *         or {@code tenantId} is invalid.
     */
    FamilyDashboardResponse getFamilyDashboard(String familyId, String tenantId, RequestInfo requestInfo);
}
