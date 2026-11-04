// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model.anomaly;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(AnomalyType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        AnomalyType first = AnomalyType.values()[0];
        assertThat(AnomalyType.valueOf(first.name())).isEqualTo(first);
    }
}
