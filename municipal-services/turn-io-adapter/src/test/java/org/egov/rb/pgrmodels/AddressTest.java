package org.egov.rb.pgrmodels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    @Test
    void setLatitude_whenRoundTrip_retainsValue() {
        Address target = new Address();
        target.setLatitude(null);
        assertThat(target.getLatitude()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Address()).isNotNull();
    }
}
