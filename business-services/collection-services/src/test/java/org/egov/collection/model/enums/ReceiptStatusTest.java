package org.egov.collection.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptStatusTest {
    @Test
    void values_nonEmpty() {
        assertThat(ReceiptStatus.values()).isNotEmpty();
    }
}
