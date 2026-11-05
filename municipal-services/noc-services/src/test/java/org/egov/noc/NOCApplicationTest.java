// generated-by: generate_per_class_unit_tests.py
package org.egov.noc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NOCApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(NOCApplication.class).isNotNull();
        assertThat(NOCApplication.class.getSimpleName()).isEqualTo("NOCApplication");
    }
}
