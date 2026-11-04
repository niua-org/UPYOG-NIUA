// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.calculator.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorConstantsTest {

    @Test
    void INVALID_TENANT_ID_MDMS_KEY_whenRead_isNotBlank() {
        assertThat(CalculatorConstants.INVALID_TENANT_ID_MDMS_KEY).isNotBlank();
    }

}
