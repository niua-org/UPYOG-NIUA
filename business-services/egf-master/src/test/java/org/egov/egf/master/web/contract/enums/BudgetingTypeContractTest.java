package org.egov.egf.master.web.contract.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetingTypeContractTest {
    @Test
    void values_nonEmpty() {
        assertThat(BudgetingTypeContract.values()).isNotEmpty();
    }
}
