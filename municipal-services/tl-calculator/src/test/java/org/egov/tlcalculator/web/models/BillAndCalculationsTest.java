package org.egov.tlcalculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillAndCalculationsTest {

    @Test
    void setBillResponse_whenRoundTrip_retainsValue() {
        BillAndCalculations target = new BillAndCalculations();
        target.setBillResponse(null);
        assertThat(target.getBillResponse()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new BillAndCalculations()).isNotNull();
    }
}
