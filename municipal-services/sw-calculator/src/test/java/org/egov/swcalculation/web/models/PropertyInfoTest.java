package org.egov.swcalculation.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyInfoTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        PropertyInfo target = new PropertyInfo();
        target.setId("test");
        assertThat(target.getId()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new PropertyInfo()).isNotNull();
    }
}
