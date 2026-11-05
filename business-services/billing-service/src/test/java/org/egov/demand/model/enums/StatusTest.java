package org.egov.demand.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StatusTest {
    @Test
    void values_nonEmpty() {
        assertThat(Status.values()).isNotEmpty();
    }
}
