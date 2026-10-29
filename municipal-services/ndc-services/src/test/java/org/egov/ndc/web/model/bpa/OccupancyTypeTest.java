// generated-by: generate_per_class_unit_tests.py
package org.egov.ndc.web.model.bpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OccupancyTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(OccupancyType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        OccupancyType first = OccupancyType.values()[0];
        assertThat(OccupancyType.valueOf(first.name())).isEqualTo(first);
    }
}
