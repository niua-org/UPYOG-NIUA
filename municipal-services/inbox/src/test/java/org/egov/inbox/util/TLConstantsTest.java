package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TLConstantsTest {

    @Test
    void TL_whenRead_isNotBlank() {
        assertThat(TLConstants.TL).isNotBlank();
    }

}
