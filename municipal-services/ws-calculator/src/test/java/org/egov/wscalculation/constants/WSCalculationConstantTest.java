package org.egov.wscalculation.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WSCalculationConstantTest {

    @Test
    void TAXPERIOD_MASTER_KEY_whenRead_isNotBlank() {
        assertThat(WSCalculationConstant.TAXPERIOD_MASTER_KEY).isNotBlank();
    }

}
