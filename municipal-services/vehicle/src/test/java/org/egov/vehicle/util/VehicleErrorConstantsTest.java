// generated-by: generate_per_class_unit_tests.py
package org.egov.vehicle.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleErrorConstantsTest {

    @Test
    void INVALID_TENANT_ID_MDMS_KEY_whenRead_isNotBlank() {
        assertThat(VehicleErrorConstants.INVALID_TENANT_ID_MDMS_KEY).isNotBlank();
    }

}
