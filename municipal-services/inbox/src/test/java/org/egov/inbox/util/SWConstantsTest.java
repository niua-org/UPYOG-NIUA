package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SWConstantsTest {

    @Test
    void SW_whenRead_isNotBlank() {
        assertThat(SWConstants.SW).isNotBlank();
    }

}
