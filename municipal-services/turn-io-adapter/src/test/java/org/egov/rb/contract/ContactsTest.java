package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactsTest {

    @Test
    void setProfile_whenRoundTrip_retainsValue() {
        Contacts target = new Contacts();
        target.setProfile(null);
        assertThat(target.getProfile()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Contacts()).isNotNull();
    }
}
