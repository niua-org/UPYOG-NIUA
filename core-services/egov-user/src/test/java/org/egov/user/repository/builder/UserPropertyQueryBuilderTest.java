package org.egov.user.repository.builder;

import org.egov.user.config.UserConfiguration;
import org.egov.user.web.contract.asset.RuralPropertySearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying dynamic SQL generation and parameter binding in {@link UserPropertyQueryBuilder}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public class UserPropertyQueryBuilderTest {

    private UserPropertyQueryBuilder queryBuilder;
    private UserConfiguration userConfiguration;

    @BeforeEach
    public void setUp() {
        userConfiguration = mock(UserConfiguration.class);
        when(userConfiguration.getAssetSearchDefaultLimit()).thenReturn(50);
        when(userConfiguration.getAssetSearchMaxLimit()).thenReturn(100);
        when(userConfiguration.getAssetSearchDefaultOffset()).thenReturn(0);
        queryBuilder = new UserPropertyQueryBuilder(userConfiguration);
    }

    @Test
    @DisplayName("Should build default rural property search query filtering active properties")
    public void testBuildRuralPropertySearchQuery_DefaultActive() {
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .userUuid("user-uuid-1")
                .build();
        Map<String, Object> params = new HashMap<>();

        String sql = queryBuilder.buildRuralPropertySearchQuery(criteria, params);

        assertNotNull(sql);
        assertTrue(sql.contains("WHERE p.locality_type = 'RURAL'"));
        assertTrue(sql.contains("AND p.user_uuid = :user_uuid"));
        assertTrue(sql.contains("AND p.status = :status"));
        assertTrue(sql.contains("ORDER BY p.created_time DESC"));
        assertTrue(sql.contains("LIMIT :limit OFFSET :offset"));
        assertEquals("user-uuid-1", params.get("user_uuid"));
        assertEquals("ACTIVE", params.get("status"));
        assertEquals(queryBuilder.getDefaultSearchLimit(), params.get("limit"));
        assertEquals(queryBuilder.getDefaultSearchOffset(), params.get("offset"));
    }

    @Test
    @DisplayName("Should apply and cap search limit and offset when provided in criteria")
    public void testBuildRuralPropertySearchQuery_PaginationAndCapping() {
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .userUuid("user-uuid-1")
                .limit(250) // exceeds MAX_SEARCH_LIMIT (100)
                .offset(20)
                .build();
        Map<String, Object> params = new HashMap<>();

        String sql = queryBuilder.buildRuralPropertySearchQuery(criteria, params);

        assertTrue(sql.contains("LIMIT :limit OFFSET :offset"));
        assertEquals(queryBuilder.getMaxSearchLimit(), params.get("limit"));
        assertEquals(20, params.get("offset"));
    }

    @Test
    @DisplayName("Should use search pagination limits from UserConfiguration when configured")
    public void testBuildRuralPropertySearchQuery_CustomConfiguration() {
        org.egov.user.config.UserConfiguration config = org.egov.user.config.UserConfiguration.builder()
                .assetSearchDefaultLimit(25)
                .assetSearchMaxLimit(75)
                .assetSearchDefaultOffset(5)
                .build();
        UserPropertyQueryBuilder customBuilder = new UserPropertyQueryBuilder(config);
        Map<String, Object> params = new HashMap<>();
        customBuilder.buildRuralPropertySearchQuery(RuralPropertySearchCriteria.builder().userUuid("user-1").build(), params);
        assertEquals(25, params.get("limit"));
        assertEquals(5, params.get("offset"));

        Map<String, Object> cappedParams = new HashMap<>();
        customBuilder.buildRuralPropertySearchQuery(RuralPropertySearchCriteria.builder().userUuid("user-1").limit(200).build(), cappedParams);
        assertEquals(75, cappedParams.get("limit"));
    }

    @Test
    @DisplayName("Should build rural property search query with multiple user UUIDs and specific status")
    public void testBuildRuralPropertySearchQuery_MultipleUsersAndInactiveStatus() {
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .userUuids(Arrays.asList("user-1", "user-2"))
                .status("INACTIVE")
                .build();
        Map<String, Object> params = new HashMap<>();

        String sql = queryBuilder.buildRuralPropertySearchQuery(criteria, params);

        assertTrue(sql.contains("AND p.user_uuid IN (:user_uuids)"));
        assertTrue(sql.contains("AND p.status = :status"));
        assertEquals("INACTIVE", params.get("status"));
        assertEquals(Arrays.asList("user-1", "user-2"), params.get("user_uuids"));
    }

    @Test
    @DisplayName("Should omit status filter when includeDeactivated is true or status is ALL")
    public void testBuildRuralPropertySearchQuery_IncludeDeactivated() {
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .propertyId("prop-id-123")
                .includeDeactivated(true)
                .build();
        Map<String, Object> params = new HashMap<>();

        String sql = queryBuilder.buildRuralPropertySearchQuery(criteria, params);

        assertTrue(sql.contains("AND (p.id = :property_id OR r.property_id = :property_id)"));
        assertFalse(sql.contains("AND p.status = :status"));
        assertEquals("prop-id-123", params.get("property_id"));
        assertFalse(params.containsKey("status"));
    }

    @Test
    @DisplayName("Should build rural property search query with propertyIds matching record id or property id")
    public void testBuildRuralPropertySearchQuery_PropertyIds() {
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .propertyIds(Arrays.asList("prop-1", "prop-2"))
                .build();
        Map<String, Object> params = new HashMap<>();

        String sql = queryBuilder.buildRuralPropertySearchQuery(criteria, params);

        assertTrue(sql.contains("AND (p.id IN (:property_ids) OR r.property_id IN (:property_ids))"));
        assertEquals(Arrays.asList("prop-1", "prop-2"), params.get("property_ids"));
    }

    @Test
    @DisplayName("Should bind Gram Panchayat, Village Name, and Survey Number filters properly")
    public void testBuildRuralPropertySearchQuery_LandholdingFilters() {
        RuralPropertySearchCriteria criteria = RuralPropertySearchCriteria.builder()
                .tenantId("pg.citya")
                .gramPanchayat("Rampur")
                .villageName("Fatehpur")
                .surveyNumber("KHASRA-102")
                .build();
        Map<String, Object> params = new HashMap<>();

        String sql = queryBuilder.buildRuralPropertySearchQuery(criteria, params);

        assertTrue(sql.contains("AND p.tenant_id = :tenant_id"));
        assertTrue(sql.contains("AND LOWER(r.gram_panchayat) = LOWER(:gram_panchayat)"));
        assertTrue(sql.contains("AND LOWER(r.village_name) = LOWER(:village_name)"));
        assertTrue(sql.contains("AND r.survey_number = :survey_number"));
        assertEquals("pg.citya", params.get("tenant_id"));
        assertEquals("Rampur", params.get("gram_panchayat"));
        assertEquals("Fatehpur", params.get("village_name"));
        assertEquals("KHASRA-102", params.get("survey_number"));
    }

    @Test
    @DisplayName("Should provide correct SQL strings for property owner and status update queries")
    public void testStaticQueries() {
        assertEquals(UserPropertyQueryBuilder.UPDATE_PROPERTY_STATUS_SQL, queryBuilder.getUpdatePropertyStatusQuery());
        assertEquals(UserPropertyQueryBuilder.DEACTIVATE_PROPERTY_OWNERS_SQL, queryBuilder.getDeactivatePropertyOwnersQuery());
        assertEquals(UserPropertyQueryBuilder.SELECT_PROPERTY_OWNERS_BY_PROPERTY_IDS_SQL, queryBuilder.getSelectPropertyOwnersByPropertyIdsQuery());
        assertTrue(queryBuilder.getUpdatePropertyStatusQuery().contains("SET status = :status"));
        assertTrue(queryBuilder.getDeactivatePropertyOwnersQuery().contains("SET active = false"));
    }

    @Test
    @DisplayName("Should build properties by user query with active status")
    public void testBuildPropertiesByUserQuery_Active() {
        Map<String, Object> params = new HashMap<>();
        String sql = queryBuilder.buildPropertiesByUserQuery(true, params);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE p.user_uuid = :user_uuid"));
        assertTrue(sql.contains("AND p.status = 'ACTIVE'"));
        assertTrue(sql.contains("ORDER BY p.created_time DESC"));
    }

    @Test
    @DisplayName("Should build properties by user query with inactive status")
    public void testBuildPropertiesByUserQuery_Inactive() {
        Map<String, Object> params = new HashMap<>();
        String sql = queryBuilder.buildPropertiesByUserQuery(false, params);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE p.user_uuid = :user_uuid"));
        assertTrue(sql.contains("AND p.status = 'INACTIVE'"));
        assertTrue(sql.contains("ORDER BY p.created_time DESC"));
    }

    @Test
    @DisplayName("Should build properties by user query without status filter when active is null")
    public void testBuildPropertiesByUserQuery_AllStatus() {
        Map<String, Object> params = new HashMap<>();
        String sql = queryBuilder.buildPropertiesByUserQuery(null, params);
        assertNotNull(sql);
        assertTrue(sql.contains("WHERE p.user_uuid = :user_uuid"));
        assertFalse(sql.contains("AND p.status ="));
        assertTrue(sql.contains("ORDER BY p.created_time DESC"));
    }
}
