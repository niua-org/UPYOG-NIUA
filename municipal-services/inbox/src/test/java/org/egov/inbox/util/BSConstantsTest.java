package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BSConstantsTest {

    @Test
    void BS_WS_whenRead_isNotBlank() {
        assertThat(BSConstants.BS_WS).isNotBlank();
    }

}
