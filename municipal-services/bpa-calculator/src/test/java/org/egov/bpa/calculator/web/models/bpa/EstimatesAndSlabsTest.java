package org.egov.bpa.calculator.web.models.bpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstimatesAndSlabsTest {

    @Test
    void setEstimates_whenRoundTrip_retainsValue() {
        EstimatesAndSlabs target = new EstimatesAndSlabs();
        target.setEstimates(java.util.Collections.emptyList());
        assertThat(target.getEstimates()).isEqualTo(java.util.Collections.emptyList());
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new EstimatesAndSlabs()).isNotNull();
    }
}
