// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VendorRelationshipTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(VendorRelationshipType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        VendorRelationshipType first = VendorRelationshipType.values()[0];
        assertThat(VendorRelationshipType.valueOf(first.name())).isEqualTo(first);
    }
}
