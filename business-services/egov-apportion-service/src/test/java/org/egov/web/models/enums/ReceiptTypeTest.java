package org.egov.web.models.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReceiptTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(ReceiptType.values()).isNotEmpty();
    }
}
