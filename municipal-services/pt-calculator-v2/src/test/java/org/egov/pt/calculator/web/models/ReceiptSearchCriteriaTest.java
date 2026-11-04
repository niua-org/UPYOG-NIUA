package org.egov.pt.calculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptSearchCriteriaTest {

    @Test
    void setTenantId_whenRoundTrip_retainsValue() {
        ReceiptSearchCriteria target = new ReceiptSearchCriteria();
        target.setTenantId("test");
        assertThat(target.getTenantId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new ReceiptSearchCriteria()).isNotNull();
    }
}
