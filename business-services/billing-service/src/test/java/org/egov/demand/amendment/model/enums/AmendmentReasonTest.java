package org.egov.demand.amendment.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentReasonTest {
    @Test
    void values_nonEmpty() {
        assertThat(AmendmentReason.values()).isNotEmpty();
    }
}
