package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonConstantsTest {

    @Test
    void MOBILE_NUMBER_PARAM_whenRead_isNotBlank() {
        assertThat(CommonConstants.MOBILE_NUMBER_PARAM).isNotBlank();
    }

}
