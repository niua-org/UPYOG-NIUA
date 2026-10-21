// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetConstantsTest {

    @Test
    void ASSET_MODULE_whenRead_isNotBlank() {
        assertThat(AssetConstants.ASSET_MODULE).isNotBlank();
    }

}
