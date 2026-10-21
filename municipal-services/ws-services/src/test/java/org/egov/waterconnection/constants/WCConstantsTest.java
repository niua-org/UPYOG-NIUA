// generated-by: generate_per_class_unit_tests.py
package org.egov.waterconnection.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WCConstantsTest {

    @Test
    void JSONPATH_ROOT_whenRead_isNotBlank() {
        assertThat(WCConstants.JSONPATH_ROOT).isNotBlank();
    }

}
