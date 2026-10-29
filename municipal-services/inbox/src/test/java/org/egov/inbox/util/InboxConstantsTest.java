package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InboxConstantsTest {

    @Test
    void INBOX_QUERY_CONFIG_NAME_whenRead_isNotBlank() {
        assertThat(InboxConstants.INBOX_QUERY_CONFIG_NAME).isNotBlank();
    }

}
