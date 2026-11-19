// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.web.model.NOC.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(ApplicationType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        ApplicationType first = ApplicationType.values()[0];
        assertThat(ApplicationType.valueOf(first.name())).isEqualTo(first);
    }
}
