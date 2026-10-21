package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityHallConstantsTest {

    @Test
    void CHB_whenRead_isNotBlank() {
        assertThat(CommunityHallConstants.CHB).isNotBlank();
    }

}
