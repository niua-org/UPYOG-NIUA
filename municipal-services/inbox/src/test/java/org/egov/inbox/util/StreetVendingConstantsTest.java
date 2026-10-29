package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreetVendingConstantsTest {

    @Test
    void SV_APPLICATION_NUMBER_PARAM_whenRead_isNotBlank() {
        assertThat(StreetVendingConstants.SV_APPLICATION_NUMBER_PARAM).isNotBlank();
    }

}
