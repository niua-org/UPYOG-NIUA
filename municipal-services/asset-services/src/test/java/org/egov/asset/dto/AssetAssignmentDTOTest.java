package org.egov.asset.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetAssignmentDTOTest {

    @Test
    void setAssignmentId_whenRoundTrip_retainsValue() {
        AssetAssignmentDTO target = new AssetAssignmentDTO();
        target.setAssignmentId("test");
        assertThat(target.getAssignmentId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new AssetAssignmentDTO()).isNotNull();
    }
}
