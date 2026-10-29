// generated-by: generate_per_class_unit_tests.py
package org.egov.noc.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NocasStatusTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(NocasStatus.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        NocasStatus first = NocasStatus.values()[0];
        assertThat(NocasStatus.valueOf(first.name())).isEqualTo(first);
    }
}
