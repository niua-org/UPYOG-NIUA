package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class V1Test {

    @Test
    void setAuthor_whenRoundTrip_retainsValue() {
        V1 target = new V1();
        target.setAuthor(null);
        assertThat(target.getAuthor()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new V1()).isNotNull();
    }
}
