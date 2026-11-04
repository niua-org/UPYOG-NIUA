package org.egov.rb.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelsTest {

    @Test
    void setConfidence_whenRoundTrip_retainsValue() {
        Labels target = new Labels();
        target.setConfidence(0.75d);
        assertThat(target.getConfidence()).isEqualTo(0.75d);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Labels()).isNotNull();
    }
}
