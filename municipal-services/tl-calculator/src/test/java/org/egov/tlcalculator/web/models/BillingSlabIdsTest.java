package org.egov.tlcalculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillingSlabIdsTest {

    @Test
    void setConsumerCode_whenRoundTrip_retainsValue() {
        BillingSlabIds target = new BillingSlabIds();
        target.setConsumerCode("test");
        assertThat(target.getConsumerCode()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new BillingSlabIds()).isNotNull();
    }
}
