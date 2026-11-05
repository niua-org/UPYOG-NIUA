package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactTest {

    @Test
    void setBirthday_whenRoundTrip_retainsValue() {
        Contact target = new Contact();
        target.setBirthday(1);
        assertThat(target.getBirthday()).isEqualTo(1);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Contact()).isNotNull();
    }
}
