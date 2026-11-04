// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.anomaly.finder.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(SourceType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        SourceType first = SourceType.values()[0];
        assertThat(SourceType.valueOf(first.name())).isEqualTo(first);
    }
}
