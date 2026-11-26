package org.egov.egf.instrument.web.contract;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionTypeContractTest {
    @Test
    void values_nonEmpty() {
        assertThat(TransactionTypeContract.values()).isNotEmpty();
    }
}
