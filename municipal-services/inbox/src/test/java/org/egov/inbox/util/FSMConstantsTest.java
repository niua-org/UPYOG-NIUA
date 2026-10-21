package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FSMConstantsTest {

    @Test
    void FSM_MODULE_whenRead_isNotBlank() {
        assertThat(FSMConstants.FSM_MODULE).isNotBlank();
    }

}
