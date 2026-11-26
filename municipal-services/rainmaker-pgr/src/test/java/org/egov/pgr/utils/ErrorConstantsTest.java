// generated-by: generate_per_class_unit_tests.py
package org.egov.pgr.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorConstantsTest {

    @Test
    void NO_DATA_MSG_whenRead_isNotBlank() {
        assertThat(ErrorConstants.NO_DATA_MSG).isNotBlank();
    }

}
