package org.upyog.family.persistence.repository;

import org.upyog.family.web.contract.state.StateFamily;

/**
 * Repository interface handling staging and retrieval of State Family sync payloads (UC2).
 * Follows Dependency Inversion Principle (DIP).
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface StateFamilySyncRepository {

    /**
     * Staging save of StateFamily payload with reference identifier.
     *
     * @param referenceId generated unique reference UUID
     * @param stateFamily domain payload from state registry
     */
    void save(String referenceId, StateFamily stateFamily);

    /**
     * Retrieves StateFamily by reference ID from cache or PostgreSQL.
     *
     * @param referenceId the unique reference UUID
     * @return StateFamily entity or null if not found
     */
    StateFamily findByReferenceId(String referenceId);

    /**
     * Retrieves StateFamily by stateFamilyId from cache or PostgreSQL.
     *
     * @param stateFamilyId the state registry family identifier
     * @return StateFamily entity or null if not found
     */
    StateFamily findByStateFamilyId(String stateFamilyId);
}
