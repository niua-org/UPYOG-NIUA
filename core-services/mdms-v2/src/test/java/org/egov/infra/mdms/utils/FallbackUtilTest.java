package org.egov.infra.mdms.utils;

import net.minidev.json.JSONArray;
import org.egov.infra.mdms.model.Mdms;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackUtilTest {

    @Test
    void getSubTenantListForFallBack_splitsHierarchy() {
        assertThat(FallbackUtil.getSubTenantListForFallBack("pg.citya.ward1"))
                .containsExactly("pg.citya.ward1", "pg.citya", "pg");
    }

    @Test
    void backTrackTenantMasterDataMap_prefersNearestTenantFirst() {
        JSONArray wardData = new JSONArray();
        wardData.add("ward-value");
        JSONArray cityData = new JSONArray();
        cityData.add("city-value");

        Map<String, JSONArray> wardMasters = Map.of("schema.a", wardData);
        Map<String, JSONArray> cityMasters = Map.of("schema.a", cityData, "schema.b", cityData);

        Map<String, Map<String, JSONArray>> tenantMasterMap = new HashMap<>();
        tenantMasterMap.put("pg.citya.ward1", wardMasters);
        tenantMasterMap.put("pg.citya", cityMasters);

        Map<String, JSONArray> result = FallbackUtil.backTrackTenantMasterDataMap(tenantMasterMap, "pg.citya.ward1");

        assertThat(result.get("schema.a")).isSameAs(wardData);
        assertThat(result.get("schema.b")).isSameAs(cityData);
    }

    @Test
    void backTrackTenantMasterDataList_collectsRecordsAlongFallbackChain() {
        Mdms ward = Mdms.builder().schemaCode("common").tenantId("pg.citya.ward1").build();
        Mdms city = Mdms.builder().schemaCode("common").tenantId("pg.citya").build();
        Mdms state = Mdms.builder().schemaCode("common").tenantId("pg").build();

        List<Mdms> result = FallbackUtil.backTrackTenantMasterDataList(
                List.of(state, city, ward), "pg.citya.ward1");

        assertThat(result).containsExactly(ward, city, state);
    }
}
