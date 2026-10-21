package org.egov.rb.pgrmodels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServicesTest {

    @Test
    void setServiceCode_whenRoundTrip_retainsValue() {
        Services target = new Services();
        target.setServiceCode("test");
        assertThat(target.getServiceCode()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Services()).isNotNull();
    }
}
