// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GarbageServiceApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(GarbageServiceApplication.class).isNotNull();
        assertThat(GarbageServiceApplication.class.getSimpleName()).isEqualTo("GarbageServiceApplication");
    }
}
