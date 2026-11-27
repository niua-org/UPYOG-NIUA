package com.tarento.analytics.org.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MdmsApiMappingsTest {

    private MdmsApiMappings mappings;

    @BeforeEach
    void setUp() {
        mappings = new MdmsApiMappings();
        Map<String, List<String>> ddrTenants = new HashMap<>();
        ddrTenants.put("DDR-A", new ArrayList<>(List.of("pb.city1", "pb.city2")));
        ReflectionTestUtils.setField(mappings, "ddrTenantMapping1", ddrTenants);
        ReflectionTestUtils.setField(mappings, "ddrValueMap", ddrTenants);
    }

    @Test
    void getGroupedTenants_groupsByDdrName() {
        Map<String, List<String>> grouped = mappings.getGroupedTenants(List.of("pb.city1", "pb.city2"));

        assertThat(grouped).containsKey("DDR-A");
        assertThat(grouped.get("DDR-A")).containsExactly("pb.city1", "pb.city2");
    }

    @Test
    void getTenantIds_returnsMappedList() {
        assertThat(mappings.getTenantIds("DDR-A")).contains("pb.city1");
    }

    @Test
    void setTranslate_switchesAllMapSource() {
        mappings.setTranslate(true);
        assertThat(mappings.getAll()).containsKey("DDR-A");
    }

    @Test
    void getDDRNameByCode_returnsMappedName() {
        ReflectionTestUtils.setField(mappings, "ddrTenantMapping", java.util.Map.of("1", "Amritsar-DDR"));
        assertThat(mappings.getDDRNameByCode("1")).isEqualTo("Amritsar-DDR");
    }

    @Test
    void getDDRName_findsDdrForTenant() {
        assertThat(mappings.getDDRName("pb.city1")).isEqualTo("DDR-A");
    }

    @Test
    void valueOf_returnsNullForUnknownCode() {
        assertThat(mappings.valueOf("missing")).isNull();
    }
}
