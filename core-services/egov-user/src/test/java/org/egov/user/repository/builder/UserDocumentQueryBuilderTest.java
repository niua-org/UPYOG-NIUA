package org.egov.user.repository.builder;

import org.egov.user.web.contract.document.UserDocumentSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test suite verifying dual-write and dual-read query construction in {@link UserDocumentQueryBuilder}.
 * Validates fixes for PR Comment #8 (rolling deployment backward compatibility).
 */
class UserDocumentQueryBuilderTest {

    private UserDocumentQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new UserDocumentQueryBuilder();
    }

    @Test
    void insertQuery_containsBothUserIdAndUserUuid() {
        String insertQuery = queryBuilder.getInsertQuery();

        // Must write both user_id (bigint) and user_uuid (varchar) for rolling deploy compatibility
        assertTrue(insertQuery.contains("user_id"), "INSERT statement must include user_id column");
        assertTrue(insertQuery.contains("user_uuid"), "INSERT statement must include user_uuid column");
        assertTrue(insertQuery.contains(":user_id"), "INSERT statement must bind :user_id parameter");
        assertTrue(insertQuery.contains(":user_uuid"), "INSERT statement must bind :user_uuid parameter");

        // Conflict resolution must preserve / coalesce both columns
        assertTrue(insertQuery.contains("user_id = COALESCE(ug_user_document.user_id, EXCLUDED.user_id)"));
        assertTrue(insertQuery.contains("user_uuid = COALESCE(ug_user_document.user_uuid, EXCLUDED.user_uuid)"));
        assertTrue(insertQuery.contains("WHERE (ug_user_document.user_uuid = EXCLUDED.user_uuid OR ug_user_document.user_uuid IS NULL)"));
    }

    @Test
    void updateQuery_containsBothUserIdAndUserUuid() {
        String updateQuery = queryBuilder.getUpdateQuery();

        assertTrue(updateQuery.contains("user_id = COALESCE(ug_user_document.user_id, :user_id)"));
        assertTrue(updateQuery.contains("user_uuid = COALESCE(ug_user_document.user_uuid, :user_uuid)"));
    }

    @Test
    void searchBySingleUserUuid_matchesBothUserUuidAndUserId() {
        UserDocumentSearchCriteria criteria = UserDocumentSearchCriteria.builder()
                .userUuid("user-uuid-123")
                .build();
        Map<String, Object> params = new HashMap<>();

        String query = queryBuilder.getDocumentSearchQuery(criteria, params);

        assertTrue(query.contains("d.user_uuid = :userUuid OR d.user_id = (SELECT id FROM eg_user WHERE uuid = :userUuid)"));
        assertTrue(params.containsKey("userUuid"));
    }

    @Test
    void searchByUserUuidList_matchesBothUserUuidsAndUserIds() {
        UserDocumentSearchCriteria criteria = UserDocumentSearchCriteria.builder()
                .userUuids(Collections.singletonList("user-uuid-list-123"))
                .build();
        Map<String, Object> params = new HashMap<>();

        String query = queryBuilder.getDocumentSearchQuery(criteria, params);

        assertTrue(query.contains("d.user_uuid IN (:userUuids) OR d.user_id IN (SELECT id FROM eg_user WHERE uuid IN (:userUuids))"));
        assertTrue(params.containsKey("userUuids"));
    }

    @Test
    void searchByUserId_matchesBothUserIdAndUserUuid() {
        UserDocumentSearchCriteria criteria = UserDocumentSearchCriteria.builder()
                .userId(555L)
                .build();
        Map<String, Object> params = new HashMap<>();

        String query = queryBuilder.getDocumentSearchQuery(criteria, params);

        assertTrue(query.contains("d.user_id = :userId OR d.user_uuid = (SELECT uuid FROM eg_user WHERE id = :userId)"));
        assertTrue(params.containsKey("userId"));
    }

    @Test
    void naturalKeyLookupQueries_checkBothUserUuidAndUserId() {
        assertTrue(UserDocumentQueryBuilder.SELECT_DOCUMENT_ID_BY_USER_AND_FILESTORE_SQL
                .contains("user_uuid = :user_uuid OR user_id = (SELECT id FROM eg_user WHERE uuid = :user_uuid)"));

        assertTrue(UserDocumentQueryBuilder.SELECT_DOCUMENT_ID_BY_USER_AND_REF_SQL
                .contains("user_uuid = :user_uuid OR user_id = (SELECT id FROM eg_user WHERE uuid = :user_uuid)"));

        assertTrue(UserDocumentQueryBuilder.SELECT_DOCUMENT_USER_UUID_BY_ID_SQL
                .contains("COALESCE(d.user_uuid, u.uuid)"));
    }
}
