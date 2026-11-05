package org.egov.garbageservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceConstantsTest {

    @Test
    void PATTERN_NAME_whenRead_isNotBlank() {
        assertThat(UserServiceConstants.PATTERN_NAME).isNotBlank();
    }

}
