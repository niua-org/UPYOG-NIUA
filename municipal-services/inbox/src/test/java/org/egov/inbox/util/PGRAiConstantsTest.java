package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PGRAiConstantsTest {

    @Test
    void SERVICE_CODE_whenRead_isNotBlank() {
        assertThat(PGRAiConstants.SERVICE_CODE).isNotBlank();
    }

}
