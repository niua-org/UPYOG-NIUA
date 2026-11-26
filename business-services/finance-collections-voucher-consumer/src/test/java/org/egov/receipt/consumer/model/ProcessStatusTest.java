package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessStatusTest {
    @Test
    void values_nonEmpty() {
        assertThat(ProcessStatus.values()).isNotEmpty();
    }
}
