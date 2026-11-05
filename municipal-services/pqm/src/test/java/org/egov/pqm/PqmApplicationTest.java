// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PqmApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(PqmApplication.class).isNotNull();
        assertThat(PqmApplication.class.getSimpleName()).isEqualTo("PqmApplication");
    }
}
