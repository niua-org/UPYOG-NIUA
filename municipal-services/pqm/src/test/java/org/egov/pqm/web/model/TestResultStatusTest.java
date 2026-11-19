// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestResultStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(TestResultStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        TestResultStatus first = TestResultStatus.values()[0];
        assertThat(TestResultStatus.valueOf(first.name())).isEqualTo(first);
    }
}
