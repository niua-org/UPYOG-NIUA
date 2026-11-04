package org.egov.tlcalculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeeAndBillingSlabIdsTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        FeeAndBillingSlabIds target = new FeeAndBillingSlabIds();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new FeeAndBillingSlabIds()).isNotNull();
    }
}
