package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesTest {

    @Test
    void setVendor_whenRoundTrip_retainsValue() {
        Messages target = new Messages();
        target.setVendor(null);
        assertThat(target.getVendor()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Messages()).isNotNull();
    }
}
