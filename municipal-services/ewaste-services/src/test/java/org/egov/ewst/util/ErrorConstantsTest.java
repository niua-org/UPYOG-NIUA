// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorConstantsTest {

    @Test
    void EG_PT_REQUESTINFO_KEY_whenRead_isNotBlank() {
        assertThat(ErrorConstants.EG_PT_REQUESTINFO_KEY).isNotBlank();
    }

}
