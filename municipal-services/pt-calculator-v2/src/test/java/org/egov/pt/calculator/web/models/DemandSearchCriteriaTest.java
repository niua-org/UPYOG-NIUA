package org.egov.pt.calculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandSearchCriteriaTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        DemandSearchCriteria target = new DemandSearchCriteria();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new DemandSearchCriteria()).isNotNull();
    }
}
