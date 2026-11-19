package org.upyog.dashboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HierarchyParserTest {

    private final HierarchyParser parser = new HierarchyParser("defaultWard", "defaultRegion", "pg");

    @Test
    @DisplayName("parseTenantId uses defaults when tenant id is blank")
    void parseTenantId_blankTenant_usesDefaults() {
        Map<String, String> hierarchy = parser.parseTenantId("  ");

        assertThat(hierarchy.get("state")).isEqualTo("pg");
        assertThat(hierarchy.get("ulb")).isEqualTo("pg");
        assertThat(hierarchy.get("region")).isEqualTo("defaultRegion");
        assertThat(hierarchy.get("ward")).isEqualTo("defaultWard");
    }

    @Test
    @DisplayName("parseTenantId expands single-segment tenant into state-prefixed ULB")
    void parseTenantId_singleSegment_buildsUlb() {
        Map<String, String> hierarchy = parser.parseTenantId("citya");

        assertThat(hierarchy.get("state")).isEqualTo("pg");
        assertThat(hierarchy.get("ulb")).isEqualTo("pg.citya");
    }

    @Test
    @DisplayName("parseTenantId parses full dot-notation hierarchy")
    void parseTenantId_fullHierarchy_parsesAllLevels() {
        Map<String, String> hierarchy = parser.parseTenantId("pg.citya.region1.ward1");

        assertThat(hierarchy.get("state")).isEqualTo("pg");
        assertThat(hierarchy.get("ulb")).isEqualTo("pg.citya");
        assertThat(hierarchy.get("region")).isEqualTo("region1");
        assertThat(hierarchy.get("ward")).isEqualTo("ward1");
    }

    @Test
    @DisplayName("parseTenantId falls back to default region and ward when segments missing")
    void parseTenantId_partialHierarchy_usesDefaultsForMissingLevels() {
        Map<String, String> hierarchy = parser.parseTenantId("pg.citya");

        assertThat(hierarchy.get("region")).isEqualTo("defaultRegion");
        assertThat(hierarchy.get("ward")).isEqualTo("defaultWard");
    }
}
