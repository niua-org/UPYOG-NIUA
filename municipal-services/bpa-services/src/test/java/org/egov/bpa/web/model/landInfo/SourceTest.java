// generated-by: generate_per_class_unit_tests.py
package org.egov.bpa.web.model.landInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(Source.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        Source first = Source.values()[0];
        assertThat(Source.valueOf(first.name())).isEqualTo(first);
    }
}
