package org.upyog.family.persistence.repository;

import org.apache.commons.lang3.StringUtils;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyLineage;
import org.upyog.family.web.contract.FamilySearchCriteria;

import java.util.List;
import java.util.Optional;

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
     * Looks up an individual Family aggregate by its unique familyId and tenantId.
     *
     * @param familyId unique family identifier
     * @param tenantId tenant identifier
     * @return Optional containing the Family aggregate if found, or empty Optional otherwise
     */
    default Optional<Family> findByFamilyIdAndTenantId(String familyId, String tenantId) {
        if (StringUtils.isBlank(familyId) || StringUtils.isBlank(tenantId)) {
            return Optional.empty();
        }
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .familyId(familyId)
                .tenantId(tenantId)
                .limit(1)
                .offset(0)
                .build();
        List<Family> families = search(criteria);
        return (families != null && !families.isEmpty()) ? Optional.of(families.get(0)) : Optional.empty();
    }

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
