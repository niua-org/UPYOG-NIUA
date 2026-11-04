package org.egov.tl.web.models.property;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyTest {

    @Test
    void setAcknowldgementNumber_whenRoundTrip_retainsValue() {
        Property target = new Property();
        target.setAcknowldgementNumber("test");
        assertThat(target.getAcknowldgementNumber()).isEqualTo("test");
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Property()).isNotNull();
    }
}
