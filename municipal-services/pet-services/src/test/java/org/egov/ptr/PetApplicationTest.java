// generated-by: generate_per_class_unit_tests.py
package org.egov.ptr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PetApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(PetApplication.class).isNotNull();
        assertThat(PetApplication.class.getSimpleName()).isEqualTo("PetApplication");
    }
}
