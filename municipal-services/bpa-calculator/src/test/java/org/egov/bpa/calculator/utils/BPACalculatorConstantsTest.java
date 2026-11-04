package org.egov.bpa.calculator.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BPACalculatorConstantsTest {

    @Test
    void MDMS_EGF_MASTER_whenRead_isNotBlank() {
        assertThat(BPACalculatorConstants.MDMS_EGF_MASTER).isNotBlank();
    }

}
