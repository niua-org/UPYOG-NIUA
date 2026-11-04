// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianRelationTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(GuardianRelation.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        GuardianRelation first = GuardianRelation.values()[0];
        assertThat(GuardianRelation.valueOf(first.name())).isEqualTo(first);
    }
}
