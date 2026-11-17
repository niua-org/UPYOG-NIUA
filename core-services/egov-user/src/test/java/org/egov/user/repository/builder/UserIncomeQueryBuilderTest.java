package org.egov.user.repository.builder;

import org.egov.user.web.contract.asset.IncomeSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying SQL query generation in {@link UserIncomeQueryBuilder}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class UserIncomeQueryBuilderTest {

    private UserIncomeQueryBuilder queryBuilder;

    @BeforeEach
    public void setUp() {
        queryBuilder = new UserIncomeQueryBuilder();
    }

    @Test
    @DisplayName("Should build income by user query with active status filter")
    public void testBuildIncomeByUserQuery_Active() {
        String sql = queryBuilder.buildIncomeByUserQuery(true);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE i.user_uuid = :user_uuid"));
        assertTrue(sql.contains("AND i.status = 'ACTIVE'"));
        assertTrue(sql.contains("ORDER BY i.created_time DESC LIMIT 1"));
    }

    @Test
    @DisplayName("Should build income by user query with inactive status filter")
    public void testBuildIncomeByUserQuery_Inactive() {
        String sql = queryBuilder.buildIncomeByUserQuery(false);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE i.user_uuid = :user_uuid"));
        assertTrue(sql.contains("AND i.status = 'INACTIVE'"));
        assertTrue(sql.contains("ORDER BY i.created_time DESC LIMIT 1"));
    }

    @Test
    @DisplayName("Should build income by user query without status filter when active is null")
    public void testBuildIncomeByUserQuery_AllStatus() {
        String sql = queryBuilder.buildIncomeByUserQuery(null);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE i.user_uuid = :user_uuid"));
        assertFalse(sql.contains("AND i.status ="));
        assertTrue(sql.contains("ORDER BY i.created_time DESC LIMIT 1"));
    }

    @Test
    @DisplayName("Should return valid static insert and audit SQL queries")
    public void testStaticQueries() {
        assertNotNull(queryBuilder.getInsertIncomeQuery());
        assertTrue(queryBuilder.getInsertIncomeQuery().contains("INSERT INTO ug_user_income"));
        assertNotNull(queryBuilder.getInsertIncomeAuditQuery());
        assertTrue(queryBuilder.getInsertIncomeAuditQuery().contains("INSERT INTO ug_user_income_audit"));
    }

    @Test
    @DisplayName("Should build default active status search query with default limit and offset")
    public void testBuildSearchQuery_DefaultLimitAndOffset() {
        Map<String, Object> params = new HashMap<>();
        IncomeSearchCriteria criteria = IncomeSearchCriteria.builder()
                .userUuid("user-uuid-001")
                .tenantId("pg.citya")
                .build();

        String sql = queryBuilder.buildSearchQuery(criteria, params);

        assertNotNull(sql);
        assertTrue(sql.contains("i.user_uuid = :user_uuid"));
        assertTrue(sql.contains("i.tenant_id = :tenant_id"));
        assertTrue(sql.contains("LIMIT :limit OFFSET :offset"));
        assertEquals("user-uuid-001", params.get("user_uuid"));
        assertEquals("pg.citya", params.get("tenant_id"));
        assertEquals(UserIncomeQueryBuilder.DEFAULT_SEARCH_LIMIT, params.get("limit"));
        assertEquals(UserIncomeQueryBuilder.DEFAULT_SEARCH_OFFSET, params.get("offset"));
    }

    @Test
    @DisplayName("Should cap search limit to MAX_SEARCH_LIMIT when input exceeds cap")
    public void testBuildSearchQuery_CapSearchLimit() {
        Map<String, Object> params = new HashMap<>();
        IncomeSearchCriteria criteria = IncomeSearchCriteria.builder()
                .userUuid("user-uuid-001")
                .limit(500)
                .offset(25)
                .build();

        String sql = queryBuilder.buildSearchQuery(criteria, params);

        assertTrue(sql.contains("LIMIT :limit OFFSET :offset"));
        assertEquals(UserIncomeQueryBuilder.MAX_SEARCH_LIMIT, params.get("limit"));
        assertEquals(25, params.get("offset"));
    }
}
