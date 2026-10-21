package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PTConstantsTest {

    @Test
    void PT_whenRead_isNotBlank() {
        assertThat(PTConstants.PT).isNotBlank();
    }

}
