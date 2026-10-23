// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.calculator.web.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(AssessmentStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        AssessmentStatus first = AssessmentStatus.values()[0];
        assertThat(AssessmentStatus.valueOf(first.name())).isEqualTo(first);
    }
}
