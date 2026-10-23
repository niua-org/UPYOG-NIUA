package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorConstantsTest {

    @Test
    void PARSING_ERROR_whenRead_isNotBlank() {
        assertThat(ErrorConstants.PARSING_ERROR).isNotBlank();
    }

}
