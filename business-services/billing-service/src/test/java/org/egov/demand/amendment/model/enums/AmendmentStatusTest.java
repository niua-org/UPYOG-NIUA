package org.egov.demand.amendment.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AmendmentStatusTest {
    @Test
    void values_nonEmpty() {
        assertThat(AmendmentStatus.values()).isNotEmpty();
    }
}
