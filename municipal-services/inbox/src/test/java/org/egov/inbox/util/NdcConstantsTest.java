package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NdcConstantsTest {

    @Test
    void NDC_BUSINESS_SERVICE_whenRead_isNotBlank() {
        assertThat(NdcConstants.NDC_BUSINESS_SERVICE).isNotBlank();
    }

}
