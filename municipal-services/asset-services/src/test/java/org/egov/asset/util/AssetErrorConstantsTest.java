// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetErrorConstantsTest {

    @Test
    void BPA_UNKNOWN_DOCS_whenRead_isNotBlank() {
        assertThat(AssetErrorConstants.BPA_UNKNOWN_DOCS).isNotBlank();
    }

}
