package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PTRConstantsTest {

    @Test
    void PTR_whenRead_isNotBlank() {
        assertThat(PTRConstants.PTR).isNotBlank();
    }

}
