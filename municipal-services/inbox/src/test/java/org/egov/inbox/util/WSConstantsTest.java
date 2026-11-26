package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WSConstantsTest {

    @Test
    void WS_whenRead_isNotBlank() {
        assertThat(WSConstants.WS).isNotBlank();
    }

}
