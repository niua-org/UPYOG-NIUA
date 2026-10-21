// generated-by: generate_per_class_unit_tests.py
package org.egov.swservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SWConstantsTest {

    @Test
    void JSONPATH_ROOT_whenRead_isNotBlank() {
        assertThat(SWConstants.JSONPATH_ROOT).isNotBlank();
    }

}
