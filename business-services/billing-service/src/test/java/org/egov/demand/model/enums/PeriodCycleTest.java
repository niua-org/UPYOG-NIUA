package org.egov.demand.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PeriodCycleTest {
    @Test
    void values_nonEmpty() {
        assertThat(PeriodCycle.values()).isNotEmpty();
    }
}
