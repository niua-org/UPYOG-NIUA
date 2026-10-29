package org.egov.tlcalculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationSearchCriteriaTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        CalculationSearchCriteria target = new CalculationSearchCriteria();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new CalculationSearchCriteria()).isNotNull();
    }
}
