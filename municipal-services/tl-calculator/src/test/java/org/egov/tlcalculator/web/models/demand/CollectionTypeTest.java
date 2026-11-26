// generated-by: generate_per_class_unit_tests.py
package org.egov.tlcalculator.web.models.demand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(CollectionType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        CollectionType first = CollectionType.values()[0];
        assertThat(CollectionType.valueOf(first.name())).isEqualTo(first);
    }
}
