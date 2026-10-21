// generated-by: generate_per_class_unit_tests.py
package org.egov.garbageservice.web.models.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseStatusEnumTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(ResponseStatusEnum.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        ResponseStatusEnum first = ResponseStatusEnum.values()[0];
        assertThat(ResponseStatusEnum.valueOf(first.name())).isEqualTo(first);
    }
}
