package org.upyog.rs.web.models.billing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaxHeadMasterTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        TaxHeadMaster target = new TaxHeadMaster();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new TaxHeadMaster()).isNotNull();
    }
}
