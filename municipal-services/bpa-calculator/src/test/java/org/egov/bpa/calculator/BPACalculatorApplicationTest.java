// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.calculator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BPACalculatorApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(BPACalculatorApplication.class).isNotNull();
        assertThat(BPACalculatorApplication.class.getSimpleName()).isEqualTo("BPACalculatorApplication");
    }
}
