package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NocConstantsTest {

    @Test
    void NOC_whenRead_isNotBlank() {
        assertThat(NocConstants.NOC).isNotBlank();
    }

}
