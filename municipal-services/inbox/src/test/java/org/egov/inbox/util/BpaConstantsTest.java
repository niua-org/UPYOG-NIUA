package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BpaConstantsTest {

    @Test
    void BPA_whenRead_isNotBlank() {
        assertThat(BpaConstants.BPA).isNotBlank();
    }

}
