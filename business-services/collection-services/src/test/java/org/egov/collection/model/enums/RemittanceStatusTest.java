package org.egov.collection.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RemittanceStatusTest {
    @Test
    void values_nonEmpty() {
        assertThat(RemittanceStatus.values()).isNotEmpty();
    }
}
