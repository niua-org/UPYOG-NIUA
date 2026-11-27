package org.egov.hrms.model.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GenderTest {
    @Test
    void values_nonEmpty() {
        assertThat(Gender.values()).isNotEmpty();
    }
}
