package org.egov.rb.pgrmodels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressDetailTest {

    @Test
    void setLatitude_whenRoundTrip_retainsValue() {
        AddressDetail target = new AddressDetail();
        target.setLatitude("test");
        assertThat(target.getLatitude()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new AddressDetail()).isNotNull();
    }
}
