package org.egov.wscalculation.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaxPeriodTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        TaxPeriod target = new TaxPeriod();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new TaxPeriod()).isNotNull();
    }
}
