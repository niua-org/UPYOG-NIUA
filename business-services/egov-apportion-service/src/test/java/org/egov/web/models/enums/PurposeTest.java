package org.egov.web.models.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PurposeTest {
    @Test
    void values_nonEmpty() {
        assertThat(Purpose.values()).isNotEmpty();
    }
}
