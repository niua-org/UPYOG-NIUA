package org.egov.egf.master.domain.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetAccountTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(BudgetAccountType.values()).isNotEmpty();
    }
}
