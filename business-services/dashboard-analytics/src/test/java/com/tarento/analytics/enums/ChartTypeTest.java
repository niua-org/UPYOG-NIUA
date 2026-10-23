package com.tarento.analytics.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChartTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(ChartType.values()).isNotEmpty();
    }
}
