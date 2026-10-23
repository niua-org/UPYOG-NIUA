// generated-by: generate_per_class_unit_tests.py
package org.egov.vendor.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleNameEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(ModuleNameEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        ModuleNameEnum first = ModuleNameEnum.values()[0];
        assertThat(ModuleNameEnum.valueOf(first.name())).isEqualTo(first);
    }
}
