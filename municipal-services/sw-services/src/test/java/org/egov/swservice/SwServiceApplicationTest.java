// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwServiceApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(SwServiceApplication.class).isNotNull();
        assertThat(SwServiceApplication.class.getSimpleName()).isEqualTo("SwServiceApplication");
    }
}
