package org.egov.pt.calculator.web.models.property;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyTest {

    @Test
    void setAuditDetails_whenRoundTrip_retainsValue() {
        Property target = new Property();
        target.setAuditDetails(null);
        assertThat(target.getAuditDetails()).isEqualTo(null);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Property()).isNotNull();
    }
}
