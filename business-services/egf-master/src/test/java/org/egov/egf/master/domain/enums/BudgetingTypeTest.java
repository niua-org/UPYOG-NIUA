package org.egov.egf.master.domain.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetingTypeTest {
    @Test
    void values_nonEmpty() {
        assertThat(BudgetingType.values()).isNotEmpty();
    }
}
