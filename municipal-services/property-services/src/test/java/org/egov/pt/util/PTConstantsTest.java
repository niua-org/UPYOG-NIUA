// generated-by: generate_per_class_unit_tests.py
package org.egov.pt.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PTConstantsTest {

    @Test
    void PT_TYPE_VACANT_whenRead_isNotBlank() {
        assertThat(PTConstants.PT_TYPE_VACANT).isNotBlank();
    }

}
