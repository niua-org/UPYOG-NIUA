package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InstrumentStatusEnumTest {
    @Test
    void values_nonEmpty() {
        assertThat(InstrumentStatusEnum.values()).isNotEmpty();
    }
}
