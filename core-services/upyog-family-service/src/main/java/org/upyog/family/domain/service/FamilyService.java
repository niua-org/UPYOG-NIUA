package org.upyog.family.domain.service;

import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.web.contract.Family;
import org.upyog.family.web.contract.FamilyMergeRequest;
import org.upyog.family.web.contract.FamilyRequest;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.upyog.family.web.contract.FamilySplitRequest;

import java.util.List;

/**
 * Domain service contract for managing citizen family units and household evolution in UPYOG.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface FamilyService {

    /**
     * Creates a new household Family Unit in UPYOG with sequential ID generation, audit logging, and persistence.
     * Validates that the head of family exists and that members are not active in any other family unit.
     *
     * @param request the family creation request containing family metadata, head of family, and member list
     * @return the created Family aggregate
     */
    Family createFamily(FamilyRequest request);

    /**
     * Searches Family Units in UPYOG based on query parameters such as family ID, member UUID, mobile, or tenant.
     * Enriches matched families with demographic profiles from the citizen identity registry.
     *
     * @param criteria search criteria containing query filter attributes
     * @param requestInfo UPYOG request context envelope
     * @return list of matching Family aggregates
     */
    List<Family> searchFamilies(FamilySearchCriteria criteria, RequestInfo requestInfo);

    /**
     * Updates an existing Family Unit's metadata and active member roster in UPYOG.
     * Validates relationships, manages soft deletes, and verifies that exactly one active Head of Family remains.
     *
     * @param request the family update request containing modified family attributes
     * @return the updated Family aggregate
     */
    Family updateFamily(FamilyRequest request);

    /**
     * Splits an existing Family Unit into a child family in UPYOG, recording full lineage genealogy.
     * Deactivates splitting members from the parent family and provisions a new family unit with the specified head.
     *
     * @param request the family split request containing parent family ID and splitting member UUIDs
     * @return the newly formed child Family aggregate
     */
    Family splitFamily(FamilySplitRequest request);

    /**
     * Merges two existing Family Units into a consolidated household unit in UPYOG.
     * Migrates members from the merging family into the surviving family, marks the merging family as MERGED, and records lineage.
     *
     * @param request the family merge request containing surviving family ID and merging family ID
     * @return the consolidated surviving Family aggregate
     */
    Family mergeFamily(FamilyMergeRequest request);
}
