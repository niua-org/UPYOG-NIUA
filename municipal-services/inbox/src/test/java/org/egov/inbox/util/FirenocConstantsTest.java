package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FirenocConstantsTest {

    @Test
    void FIRENOC_SERVICE_whenRead_isNotBlank() {
        assertThat(FirenocConstants.FIRENOC_SERVICE).isNotBlank();
    }

}
