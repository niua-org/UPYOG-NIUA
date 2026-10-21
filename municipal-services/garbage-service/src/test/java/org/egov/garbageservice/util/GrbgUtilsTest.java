package org.egov.garbageservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrbgUtilsTest {

    @Test
    @DisplayName("getCurrentTimestamp_whenCalled_returnsPositiveValue")
    void getCurrentTimestamp_whenCalled_returnsPositiveValue() {
        assertThat(GrbgUtils.getCurrentTimestamp()).isPositive();
    }
}
