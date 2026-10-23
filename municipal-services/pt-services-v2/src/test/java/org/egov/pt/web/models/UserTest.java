package org.egov.pt.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        User target = new User();
        target.setId(1L);
        assertThat(target.getId()).isEqualTo(1L);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new User()).isNotNull();
    }
}
