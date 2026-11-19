// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NDCApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(NDCApplication.class).isNotNull();
        assertThat(NDCApplication.class.getSimpleName()).isEqualTo("NDCApplication");
    }
}
