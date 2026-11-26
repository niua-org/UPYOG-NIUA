// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BPAApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(BPAApplication.class).isNotNull();
        assertThat(BPAApplication.class.getSimpleName()).isEqualTo("BPAApplication");
    }
}
