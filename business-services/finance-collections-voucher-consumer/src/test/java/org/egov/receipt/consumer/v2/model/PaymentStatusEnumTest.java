package org.egov.receipt.consumer.v2.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusEnumTest {
    @Test
    void values_nonEmpty() {
        assertThat(PaymentStatusEnum.values()).isNotEmpty();
    }
}
