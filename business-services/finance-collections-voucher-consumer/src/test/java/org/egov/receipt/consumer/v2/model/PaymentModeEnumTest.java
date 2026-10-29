package org.egov.receipt.consumer.v2.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentModeEnumTest {

    @Test
    void fromValue_knownMode_returnsEnum() {
        assertThat(PaymentModeEnum.fromValue("CASH")).isEqualTo(PaymentModeEnum.CASH);
    }

    @Test
    void contains_knownMode_isTrue() {
        assertThat(PaymentModeEnum.contains("ONLINE")).isTrue();
    }

    @Test
    void contains_unknownMode_isFalse() {
        assertThat(PaymentModeEnum.contains("UNKNOWN_MODE")).isFalse();
    }
}
