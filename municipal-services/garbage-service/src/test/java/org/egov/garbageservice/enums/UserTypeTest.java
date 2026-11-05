// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(UserType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        UserType first = UserType.values()[0];
        assertThat(UserType.valueOf(first.name())).isEqualTo(first);
    }
}
