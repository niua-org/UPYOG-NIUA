package org.upyog.adv.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlCodeMasterTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        GlCodeMaster target = new GlCodeMaster();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new GlCodeMaster()).isNotNull();
    }
}
