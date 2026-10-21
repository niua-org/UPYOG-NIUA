package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestServiceConstantsTest {

    @Test
    void BOOKING_NO_PARAM_whenRead_isNotBlank() {
        assertThat(RequestServiceConstants.BOOKING_NO_PARAM).isNotBlank();
    }

}
