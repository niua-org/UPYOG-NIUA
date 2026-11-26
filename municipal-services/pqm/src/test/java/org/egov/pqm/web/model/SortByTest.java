// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SortByTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(SortBy.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        SortBy first = SortBy.values()[0];
        assertThat(SortBy.valueOf(first.name())).isEqualTo(first);
    }
}
