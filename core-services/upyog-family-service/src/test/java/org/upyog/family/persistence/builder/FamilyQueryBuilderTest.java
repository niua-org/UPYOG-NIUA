package org.upyog.family.persistence.builder;

import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.domain.model.FamilyStatus;
import org.upyog.family.web.contract.FamilySearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FamilyQueryBuilderTest {

    @Mock
    private FamilyConfiguration configuration;

    @InjectMocks
    private FamilyQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(configuration.getDefaultOffset()).thenReturn(0);
        org.mockito.Mockito.lenient().when(configuration.getDefaultLimit()).thenReturn(10);
        org.mockito.Mockito.lenient().when(configuration.getMaxLimit()).thenReturn(100);
    }

    @Test
    @DisplayName("getFamilySearchQuery should build parameterized SQL with tenantId and familyId")
    void getFamilySearchQuery_BasicCriteria() {
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .tenantId("pg.citya")
                .familyId("FAM-2026-0001")
                .status(FamilyStatus.ACTIVE)
                .build();

        List<Object> params = new ArrayList<>();
        String sql = queryBuilder.getFamilySearchQuery(criteria, params);

        assertTrue(sql.contains("f.tenant_id = ?"));
        assertTrue(sql.contains("f.family_id = ?"));
        assertTrue(sql.contains("f.status = ?"));
        assertTrue(sql.contains("LIMIT ? OFFSET ?"));

        assertEquals("pg.citya", params.get(0));
        assertEquals("FAM-2026-0001", params.get(1));
        assertEquals("ACTIVE", params.get(2));
        assertEquals(10, params.get(3));
        assertEquals(0, params.get(4));
    }

    @Test
    @DisplayName("getFamilySearchQuery should build subquery when memberUserUuid is provided")
    void getFamilySearchQuery_MemberCriteria() {
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .tenantId("pg.citya")
                .memberUserUuid("user-uuid-123")
                .build();

        List<Object> params = new ArrayList<>();
        String sql = queryBuilder.getFamilySearchQuery(criteria, params);

        assertTrue(sql.contains("f.family_id IN (SELECT family_id FROM ug_family_member WHERE user_uuid = ? AND active = true)"));
        assertEquals("pg.citya", params.get(0));
        assertEquals("user-uuid-123", params.get(1));
    }

    @Test
    @DisplayName("getFamilySearchQuery should handle multiple familyIds")
    void getFamilySearchQuery_MultipleFamilyIds() {
        FamilySearchCriteria criteria = FamilySearchCriteria.builder()
                .tenantId("pg.citya")
                .familyIds(Arrays.asList("FAM-001", "FAM-002"))
                .build();

        List<Object> params = new ArrayList<>();
        String sql = queryBuilder.getFamilySearchQuery(criteria, params);

        assertTrue(sql.contains("f.family_id IN (?, ?)"));
        assertEquals("pg.citya", params.get(0));
        assertEquals("FAM-001", params.get(1));
        assertEquals("FAM-002", params.get(2));
    }
}
