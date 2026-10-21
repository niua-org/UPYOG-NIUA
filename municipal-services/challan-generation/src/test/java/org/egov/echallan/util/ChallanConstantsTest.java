// generated-by: generate_per_class_unit_tests.py
package org.egov.echallan.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChallanConstantsTest {

    @Test
    void STATUS_ACTIVE_whenRead_isNotBlank() {
        assertThat(ChallanConstants.STATUS_ACTIVE).isNotBlank();
    }

    @Test
    void constructor_whenPrivate_rejectsUtilityInstantiation() throws Exception {
        var ctor = ChallanConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(ctor::newInstance)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

}
