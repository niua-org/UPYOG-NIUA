package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadContactTest {

    @Test
    void setContact_whenRoundTrip_retainsValue() {
        ThreadContact target = new ThreadContact();
        target.setContact(null);
        assertThat(target.getContact()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new ThreadContact()).isNotNull();
    }
}
