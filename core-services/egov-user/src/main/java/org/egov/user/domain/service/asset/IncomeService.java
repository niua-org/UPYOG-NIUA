package org.egov.user.domain.service.asset;

import org.egov.common.contract.request.RequestInfo;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.web.contract.asset.IncomeRequest;
import org.egov.user.web.contract.asset.IncomeSearchCriteria;

import java.util.List;

/**
 * Domain service contract governing standalone CRUD and lifecycle management of citizen income declarations
 * in UPYOG user registry.
 *
 * <p>Supports creation, updates, querying with rich criteria, and soft-deactivation (status = INACTIVE).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface IncomeService {

    /**
     * Creates a new citizen income declaration.
     *
     * @param request income creation request payload
     * @return persisted {@link UserIncome} domain entity with populated IDs and audit timestamps
     */
    UserIncome createIncome(IncomeRequest request);

    /**
     * Updates an existing citizen income declaration.
     *
     * @param request income update request payload
     * @return updated {@link UserIncome} domain entity
     */
    UserIncome updateIncome(IncomeRequest request);

    /**
     * Searches citizen income declarations matching the specified criteria.
     *
     * @param criteria    query criteria filters
     * @param requestInfo standard UPYOG request context envelope
     * @return list of matching {@link UserIncome} entities
     */
    List<UserIncome> searchIncome(IncomeSearchCriteria criteria, RequestInfo requestInfo);

    /**
     * Deactivates / soft-deletes a citizen income declaration by setting its status to {@code INACTIVE}.
     *
     * @param request income delete/deactivation request payload
     * @return deactivated {@link UserIncome} entity with {@code INACTIVE} status
     */
    UserIncome deleteIncome(IncomeRequest request);

    /**
     * Direct deactivation helper method marking an income declaration as {@code INACTIVE}.
     *
     * @param incomeId    income record primary key ID
     * @param userUuid    citizen user UUID
     * @param requestInfo standard UPYOG request context envelope
     * @return deactivated {@link UserIncome} entity
     */
    UserIncome deactivateIncome(String incomeId, String userUuid, RequestInfo requestInfo);
}
