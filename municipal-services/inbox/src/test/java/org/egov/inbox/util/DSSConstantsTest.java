package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DSSConstantsTest {

    @Test
    void DSS_VISUALIZATIONTYPE_whenRead_isNotBlank() {
        assertThat(DSSConstants.DSS_VISUALIZATIONTYPE).isNotBlank();
    }

}
