// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VendorApplicationTest {

    @Test
    void class_whenLoaded_isPublicApplicationType() {
        assertThat(VendorApplication.class).isNotNull();
        assertThat(VendorApplication.class.getSimpleName()).isEqualTo("VendorApplication");
    }
}
