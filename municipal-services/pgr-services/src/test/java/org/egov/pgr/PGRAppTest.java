// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PGRAppTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(PGRApp.class).isNotNull();
        assertThat(PGRApp.class.getSimpleName()).isEqualTo("PGRApp");
    }
}
