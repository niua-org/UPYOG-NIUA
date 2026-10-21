package org.egov.asset.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetSearchDTOTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        AssetSearchDTO target = new AssetSearchDTO();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new AssetSearchDTO()).isNotNull();
    }
}
