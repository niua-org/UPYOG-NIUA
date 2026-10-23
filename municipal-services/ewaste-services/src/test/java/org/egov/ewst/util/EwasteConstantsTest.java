// generated-by: generate_per_class_unit_tests.py
package org.egov.ewst.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EwasteConstantsTest {

    @Test
    void MDMS_EW_MOD_NAME_whenRead_isNotBlank() {
        assertThat(EwasteConstants.MDMS_EW_MOD_NAME).isNotBlank();
    }

}
