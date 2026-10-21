package org.egov.ndc.calculator.web.models.bill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void setId_whenRoundTrip_retainsValue() {
        Category target = new Category();
        target.setId(1L);
        assertThat(target.getId()).isEqualTo(1L);
    }

    @Test
    void newInstance_whenCreated_isNotNull() {
        assertThat(new Category()).isNotNull();
    }
}
