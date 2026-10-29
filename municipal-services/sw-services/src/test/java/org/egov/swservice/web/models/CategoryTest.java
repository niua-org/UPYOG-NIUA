// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Category.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Category first = Category.values()[0];
        assertThat(Category.valueOf(first.name())).isEqualTo(first);
    }
}
