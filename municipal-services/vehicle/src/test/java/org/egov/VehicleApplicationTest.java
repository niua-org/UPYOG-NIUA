// generated-by: generate_per_class_unit_tests.py
package org.egov;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(VehicleApplication.class).isNotNull();
        assertThat(VehicleApplication.class.getSimpleName()).isEqualTo("VehicleApplication");
    }
}
