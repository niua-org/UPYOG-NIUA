package org.egov.egf.master.web.contract.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BankAccountTypeContractTest {
    @Test
    void values_nonEmpty() {
        assertThat(BankAccountTypeContract.values()).isNotEmpty();
    }
}
