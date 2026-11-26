package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EwasteConstantsTest {

    @Test
    void EWASTE_whenRead_isNotBlank() {
        assertThat(EwasteConstants.EWASTE).isNotBlank();
    }

}
