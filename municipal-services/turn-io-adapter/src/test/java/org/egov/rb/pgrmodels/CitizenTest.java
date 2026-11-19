package org.egov.rb.pgrmodels;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        Citizen target = new Citizen();
        target.setId(1L);
        assertThat(target.getId()).isEqualTo(1L);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Citizen()).isNotNull();
    }
}
