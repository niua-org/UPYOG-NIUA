package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileTest {

    @Test
    void setName_whenRoundTrip_retainsValue() {
        Profile target = new Profile();
        target.setName("test");
        assertThat(target.getName()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Profile()).isNotNull();
    }
}
