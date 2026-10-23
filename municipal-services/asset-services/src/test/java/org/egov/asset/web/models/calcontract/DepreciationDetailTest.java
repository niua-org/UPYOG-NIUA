package org.egov.asset.web.models.calcontract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepreciationDetailTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        DepreciationDetail target = new DepreciationDetail();
        target.setId(1L);
        assertThat(target.getId()).isEqualTo(1L);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new DepreciationDetail()).isNotNull();
    }
}
