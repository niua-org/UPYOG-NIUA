// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TreePruningStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(TreePruningStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        TreePruningStatus first = TreePruningStatus.values()[0];
        assertThat(TreePruningStatus.valueOf(first.name())).isEqualTo(first);
    }
}
