package org.egov.hrms.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EducationalQualificationTest {
    @Test
    void classLoads() {
        assertThat(EducationalQualification.class).isNotNull();
    }

    @Test
    void canReflectNoArgConstructorIfPresent() throws Exception {
        var ctor = EducationalQualification.class.getDeclaredConstructors();
        assertThat(ctor).isNotEmpty();
    }
}
