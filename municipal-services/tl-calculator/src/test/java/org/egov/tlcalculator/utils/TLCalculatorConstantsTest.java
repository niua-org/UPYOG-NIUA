package org.egov.tlcalculator.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TLCalculatorConstantsTest {

    @Test
    void MDMS_EGF_MASTER_whenRead_isNotBlank() {
        assertThat(TLCalculatorConstants.MDMS_EGF_MASTER).isNotBlank();
    }

}
