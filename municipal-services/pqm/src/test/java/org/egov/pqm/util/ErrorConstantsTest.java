package org.egov.pqm.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorConstantsTest {

    @Test
    void CREATE_ERROR_whenRead_isNotBlank() {
        assertThat(ErrorConstants.CREATE_ERROR).isNotBlank();
    }

}
