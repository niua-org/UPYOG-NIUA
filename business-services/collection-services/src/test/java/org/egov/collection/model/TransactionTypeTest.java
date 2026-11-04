package org.egov.collection.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(TransactionType.values()).isNotEmpty();
    }
}
