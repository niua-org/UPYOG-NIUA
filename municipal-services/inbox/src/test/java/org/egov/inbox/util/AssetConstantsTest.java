package org.egov.inbox.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetConstantsTest {

    @Test
    void ASSET_whenRead_isNotBlank() {
        assertThat(AssetConstants.ASSET).isNotBlank();
    }

}
