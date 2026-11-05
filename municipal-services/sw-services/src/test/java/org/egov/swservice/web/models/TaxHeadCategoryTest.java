// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaxHeadCategoryTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(TaxHeadCategory.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        TaxHeadCategory first = TaxHeadCategory.values()[0];
        assertThat(TaxHeadCategory.valueOf(first.name())).isEqualTo(first);
    }
}
