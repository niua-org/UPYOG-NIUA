// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.anomaly.finder.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(TypeEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        TypeEnum first = TypeEnum.values()[0];
        assertThat(TypeEnum.valueOf(first.name())).isEqualTo(first);
    }
}
