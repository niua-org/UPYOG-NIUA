package org.egov.receipt.consumer.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(BankAccountType.values()).isNotEmpty();
    }
}
