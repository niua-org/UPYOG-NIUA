package org.egov.swcalculation.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SWCalculationConstantTest {

    @Test
    void TAXPERIOD_MASTER_KEY_whenRead_isNotBlank() {
        assertThat(SWCalculationConstant.TAXPERIOD_MASTER_KEY).isNotBlank();
    }

}
