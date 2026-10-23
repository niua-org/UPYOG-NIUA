package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextTest {

    @Test
    void setBody_whenRoundTrip_retainsValue() {
        Text target = new Text();
        target.setBody("test");
        assertThat(target.getBody()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Text()).isNotNull();
    }
}
