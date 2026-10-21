package org.egov.pt.calculator.web.models.propertyV2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyInfoV2Test {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        PropertyInfoV2 target = new PropertyInfoV2();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new PropertyInfoV2()).isNotNull();
    }
}
