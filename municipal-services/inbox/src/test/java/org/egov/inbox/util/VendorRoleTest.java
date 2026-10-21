// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VendorRoleTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(VendorRole.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        VendorRole first = VendorRole.values()[0];
        assertThat(VendorRole.valueOf(first.name())).isEqualTo(first);
    }
}
