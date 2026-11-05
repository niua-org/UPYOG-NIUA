package org.egov.tl.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BPAConstantsTest {

    @Test
    void TRADETYPE_TO_ROLEMAPPING_whenRead_isNotBlank() {
        assertThat(BPAConstants.TRADETYPE_TO_ROLEMAPPING).isNotBlank();
    }

}
