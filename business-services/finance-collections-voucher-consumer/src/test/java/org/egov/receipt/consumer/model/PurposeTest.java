package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PurposeTest {
    @Test
    void values_nonEmpty() {
        assertThat(Purpose.values()).isNotEmpty();
    }
}
