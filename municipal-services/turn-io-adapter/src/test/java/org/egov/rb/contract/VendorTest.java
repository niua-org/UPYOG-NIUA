package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VendorTest {

    @Test
    void setV1_whenRoundTrip_retainsValue() {
        Vendor target = new Vendor();
        target.setV1(null);
        assertThat(target.getV1()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Vendor()).isNotNull();
    }
}
