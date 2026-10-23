// generated-by: generate_per_class_unit_tests.py
package org.egov.tl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(Main.class).isNotNull();
        assertThat(Main.class.getSimpleName()).isEqualTo("Main");
    }
}
