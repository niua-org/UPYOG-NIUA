package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChallanConstantsTest {

    @Test
    void CHALLAN_GENERATION_whenRead_isNotBlank() {
        assertThat(ChallanConstants.CHALLAN_GENERATION).isNotBlank();
    }

}
