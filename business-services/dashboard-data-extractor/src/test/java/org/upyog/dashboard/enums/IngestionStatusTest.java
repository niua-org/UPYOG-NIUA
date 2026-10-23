package org.upyog.dashboard.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IngestionStatusTest {
    @Test
    void values_nonEmpty() {
        assertThat(IngestionStatus.values()).isNotEmpty();
    }
}
