// generated-by: generate_per_class_unit_tests.py
package org.egov.pqm.web.model.plant.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantUserTypeTest {

    @Test
    void values_whenQueried_isNotEmpty() {
        assertThat(PlantUserType.values()).isNotEmpty();
    }

    @Test
    void valueOf_whenFirstConstantName_returnsConstant() {
        PlantUserType first = PlantUserType.values()[0];
        assertThat(PlantUserType.valueOf(first.name())).isEqualTo(first);
    }
}
