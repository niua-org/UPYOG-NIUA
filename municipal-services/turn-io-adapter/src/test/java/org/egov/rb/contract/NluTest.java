package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NluTest {

    @Test
    void setConfidence_whenRoundTrip_retainsValue() {
        Nlu target = new Nlu();
        target.setConfidence(0.5d);
        assertThat(target.getConfidence()).isEqualTo(0.5d);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Nlu()).isNotNull();
    }
}
