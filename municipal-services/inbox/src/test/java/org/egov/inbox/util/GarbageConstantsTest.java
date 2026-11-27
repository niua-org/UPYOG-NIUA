package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GarbageConstantsTest {

    @Test
    void GARBAGE_SERVICE_whenRead_isNotBlank() {
        assertThat(GarbageConstants.GARBAGE_SERVICE).isNotBlank();
    }

}
