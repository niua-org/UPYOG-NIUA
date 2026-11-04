package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        Image target = new Image();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Image()).isNotNull();
    }
}
