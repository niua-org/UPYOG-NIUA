package org.upyog.family.persistence.repository;

import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.List;

/**
 * Data access abstraction interface for Family unit aggregates and lineage history in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyRepository {

    /**
     * Persists a new Family aggregate along with its members and document proofs to PostgreSQL in UPYOG.
     *
     * @param family the Family aggregate to persist
     * @return the persisted Family aggregate
     */
    Family save(Family family);

    /**
     * Updates an existing Family aggregate, active member records, and document proofs in UPYOG.
     *
     * @param family the updated Family aggregate
     * @return the persisted updated Family aggregate
     */
    Family update(Family family);

    /**
     * Searches Family aggregates based on filter criteria (family ID, member UUID, or tenant ID) in UPYOG.
     *
     * @param criteria search criteria filter object
     * @return list of matching Family aggregates
     */
    List<Family> search(FamilySearchCriteria criteria);

    /**
     * Finds active Family IDs where the specified citizen UUID is registered as an active member in UPYOG.
     *
     * @param userUuid unique citizen identifier
     * @return list of active family ID strings
     */
    List<String> findActiveFamilyIdsByMemberUuid(String userUuid);

    /**
     * Records a family lifecycle lineage event (SPLIT or MERGE) into ug_family_lineage in UPYOG.
     *
     * @param lineage the lineage record object
     */
    void saveLineage(FamilyLineage lineage);

    /**
     * Retrieves all genealogical lineage events associated with a family identifier in UPYOG.
     *
     * @param familyId the unique family identifier
     * @return list of FamilyLineage event records
     */
    List<FamilyLineage> findLineageByFamilyId(String familyId);
}
