package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CNDServiceConstantsTest {

    @Test
    void APPLICATION_NO_PARAM_whenRead_isNotBlank() {
        assertThat(CNDServiceConstants.APPLICATION_NO_PARAM).isNotBlank();
    }

}
